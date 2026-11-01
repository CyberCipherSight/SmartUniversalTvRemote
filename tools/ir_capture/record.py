#!/usr/bin/env python3
"""Receive-only serial recorder. All raw serial bytes and unknown/incomplete captures stay local."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import signal
import sys
import time
import uuid
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
MAX_LINE = 131072
KNOWN_USB_IDS = {(0x10C4, 0xEA60), (0x1A86, 0x7523), (0x1A86, 0x55D4),
                 (0x303A, 0x1001), (0x0403, 0x6001)}


def now():
    return datetime.now(timezone.utc).isoformat()


def atomic_json(path, value):
    temporary = path.with_suffix(path.suffix + '.tmp')
    temporary.write_text(json.dumps(value, indent=2) + '\n')
    temporary.replace(path)


class Lines:
    def __init__(self):
        self.buffer = bytearray()
        self.discarding = False

    def feed(self, chunk):
        output = []
        for piece in chunk.splitlines(keepends=True):
            ending = piece.endswith(b'\n')
            if not self.discarding:
                self.buffer.extend(piece)
                if len(self.buffer) > MAX_LINE:
                    output.append((None, 'oversized_serial_line'))
                    self.buffer.clear()
                    self.discarding = True
                elif ending:
                    output.append((bytes(self.buffer).rstrip(b'\r\n'), None))
                    self.buffer.clear()
            if ending:
                self.discarding = False
        return output


def validate_capture(record):
    if record.get('schema_version') != 1 or record.get('event') != 'capture':
        raise ValueError('not a v1 capture')
    if not isinstance(record.get('protocol'), str) or not isinstance(record.get('boot_id'), str):
        raise ValueError('missing decoder identity')
    for field in ['sequence', 'uptime_us', 'bits', 'leading_gap_us', 'queue_dropped_total']:
        if type(record.get(field)) is not int or record[field] < 0:
            raise ValueError(f'invalid {field}')
    for field in ['repeat', 'overflow', 'timings_compensated']:
        if type(record.get(field)) is not bool:
            raise ValueError(f'invalid {field}')
    raw = record.get('raw_us')
    if not isinstance(raw, list) or len(raw) > 2047 or any(type(t) is not int or not 0 <= t <= 131070 for t in raw):
        raise ValueError('invalid raw timings')
    if record.get('carrier_hz_measured') is not None:
        raise ValueError('demodulating receiver cannot measure the carrier')
    return record


class Session:
    def __init__(self, directory, context, port='auto'):
        self.directory = Path(directory)
        self.directory.mkdir(parents=True, exist_ok=False)
        self.context_path = self.directory / 'context.json'
        atomic_json(self.context_path, context)
        atomic_json(self.directory / 'session.json', {
            'schema_version': 1, 'created_utc': now(), 'receive_only': True, 'requested_port': port,
            'source_context': context, 'verification': 'unverified_capture',
            'firmware_source_sha256': hashlib.sha256((ROOT / 'firmware/ir_learner/ir_learner.ino').read_bytes()).hexdigest(),
            'notes': 'Demodulated envelope only; receiver center frequency is not a measured carrier. Phone captures are not original-remote or speaker verification.',
        })
        self.raw = (self.directory / 'serial.bin').open('ab', buffering=0)
        self.frames = (self.directory / 'captures.jsonl').open('a', buffering=1)
        self.events = (self.directory / 'events.jsonl').open('a', buffering=1)
        self.total = 0
        self.unknown = 0
        self.invalid = 0
        self.sequences = {}
        self.framer = Lines()
        self.state = {'state': 'waiting_for_serial', 'captures': 0, 'unknown': 0, 'receive_only': True}
        self.update()

    def event(self, event, **details):
        self.events.write(json.dumps({'host_utc': now(), 'event': event, **details}) + '\n')

    def update(self, **details):
        self.state.update(details)
        self.state.update(updated_utc=now(), captures=self.total, unknown=self.unknown, invalid=self.invalid)
        atomic_json(self.directory / 'status.json', self.state)

    def feed(self, data):
        self.raw.write(data)
        for line, error in self.framer.feed(data):
            if error:
                self.invalid += 1; self.event(error)
                continue
            if not line:
                continue
            try:
                record = json.loads(line)
                if not isinstance(record, dict):
                    raise ValueError('not an object')
                if record.get('event') != 'capture':
                    self.event('firmware_event', payload=record)
                    if record.get('firmware') == 'ir-learner-1' and record.get('receive_only') is True:
                        self.update(state='listening', last_heartbeat_utc=now(), firmware=record,
                                    receiver_connection_verified=False)
                    continue
                validate_capture(record)
            except (ValueError, TypeError) as exc:
                self.invalid += 1
                self.event('unparsed_serial_line', reason=str(exc), serial_line_sha256=hashlib.sha256(line).hexdigest())
                self.update()
                continue
            try:
                context = json.loads(self.context_path.read_text())
            except (OSError, ValueError):
                context = {'kind': 'unknown', 'function': None, 'warning': 'capture_context_unavailable'}
                self.event('context_unavailable')
            boot = record['boot_id']
            previous = self.sequences.get(boot)
            if previous is None and record['sequence'] > 1:
                self.event('captures_before_first_observed', boot_id=boot, count=record['sequence'] - 1)
            if previous is not None and record['sequence'] > previous + 1:
                self.event('sequence_gap', boot_id=boot, missing_count=record['sequence'] - previous - 1)
            self.sequences[boot] = record['sequence']
            warnings = []
            if record['overflow']:
                warnings.append('capture_buffer_overflow')
            if not record['raw_us'] or 0 in record['raw_us']:
                warnings.append('empty_or_zero_duration_envelope')
            if record['queue_dropped_total']:
                warnings.append('firmware_reported_queue_loss')
            unknown = record['protocol'].upper() == 'UNKNOWN'
            if unknown:
                warnings.append('unknown_decoder_hash_is_not_a_transmittable_code')
            saved = {
                'schema_version': 1, 'capture_id': str(uuid.uuid4()), 'host_utc': now(),
                'source': context, 'verification': 'unverified_capture',
                'speaker_response_verified': False, 'carrier_hz_measured': None,
                'warnings': warnings, 'firmware': record,
                'serial_line_sha256': hashlib.sha256(line).hexdigest(),
            }
            self.frames.write(json.dumps(saved, separators=(',', ':')) + '\n')
            self.frames.flush(); os.fsync(self.frames.fileno())
            self.total += 1; self.unknown += int(unknown)
            self.update(state='listening', last_capture_utc=saved['host_utc'], last_protocol=record['protocol'],
                        last_sequence=record['sequence'], queue_dropped_total=record['queue_dropped_total'])

    def close(self):
        if self.framer.buffer:
            self.event('partial_serial_line_at_stop', bytes=len(self.framer.buffer))
        self.update(state='stopped')
        for handle in [self.raw, self.frames, self.events]:
            handle.flush(); os.fsync(handle.fileno()); handle.close()


def serial_module():
    try:
        import serial
        import serial.tools.list_ports
        return serial
    except ImportError:
        raise SystemExit('Use .tools/esp32/python/bin/python (workspace-local pyserial environment).')


def ports():
    serial = serial_module()
    return [{'port': p.device, 'description': p.description, 'vid': p.vid, 'pid': p.pid,
             'serial_number': p.serial_number, 'known_bridge': (p.vid, p.pid) in KNOWN_USB_IDS}
            for p in serial.tools.list_ports.comports()]


def run(args):
    serial = serial_module()
    directory = ROOT / 'captures/ir' / (datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%SZ') + '-' + uuid.uuid4().hex[:8])
    context = {'kind': args.source, 'brand': args.brand, 'model': args.model,
               'remote_identifier': args.remote_id, 'function': None,
               'carrier_hz_assumed': None, 'updated_utc': now()}
    session = Session(directory, context, args.port)
    print(str(directory), flush=True)
    deadline = time.monotonic() + args.duration if args.duration else None
    running = True

    def stop(*_):
        nonlocal running
        running = False
    signal.signal(signal.SIGINT, stop); signal.signal(signal.SIGTERM, stop)
    try:
        while running and (deadline is None or time.monotonic() < deadline):
            candidates = ports()
            known = [p['port'] for p in candidates if p['known_bridge']]
            port = args.port if args.port != 'auto' else known[0] if len(known) == 1 else None
            if not port:
                session.update(state='waiting_for_usb' if not known else 'ambiguous_usb_ports', ports=candidates)
                time.sleep(1)
                continue
            try:
                connection = serial.Serial(port=None, baudrate=115200, timeout=0.25, exclusive=True)
                connection.dtr = False; connection.rts = False
                connection.port = port
                connection.open()
                if session.framer.buffer or session.framer.discarding:
                    session.event('partial_line_at_reconnect', bytes=len(session.framer.buffer))
                    session.framer = Lines()
                session.event('serial_connected', port=port)
                session.update(state='waiting_for_firmware_heartbeat', port=port)
                with connection:
                    while running and (deadline is None or time.monotonic() < deadline):
                        chunk = connection.read(4096)
                        if chunk:
                            session.feed(chunk)
                session.event('serial_closed', port=port)
            except (serial.SerialException, OSError) as exc:
                session.event('serial_unavailable', port=port, reason=str(exc))
                session.update(state='waiting_for_serial', reason=str(exc))
                time.sleep(1)
    finally:
        session.close()


def annotate(args):
    path = Path(args.session)
    context = json.loads((path / 'context.json').read_text())
    if args.function is not None:
        context['function'] = args.function
    if args.source:
        context['kind'] = args.source
    if args.model:
        context['model'] = args.model
    context['updated_utc'] = now()
    atomic_json(path / 'context.json', context)
    with (path / 'context_history.jsonl').open('a') as history:
        history.write(json.dumps(context) + '\n')
    print('Context updated for subsequent captures. Wait two seconds before pressing the next button.')


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    sub = parser.add_subparsers(dest='action', required=True)
    sub.add_parser('ports')
    start = sub.add_parser('listen')
    start.add_argument('--port', default='auto')
    start.add_argument('--source', choices=['phone_app', 'original_remote', 'unknown'], default='phone_app')
    start.add_argument('--brand'); start.add_argument('--model'); start.add_argument('--remote-id')
    start.add_argument('--duration', type=float, help='Optional bounded test run, seconds')
    label = sub.add_parser('context')
    label.add_argument('session'); label.add_argument('--function'); label.add_argument('--source', choices=['phone_app', 'original_remote', 'unknown']); label.add_argument('--model')
    status = sub.add_parser('status'); status.add_argument('session')
    args = parser.parse_args()
    if args.action == 'ports': print(json.dumps(ports(), indent=2))
    elif args.action == 'listen': run(args)
    elif args.action == 'context': annotate(args)
    else: print((Path(args.session) / 'status.json').read_text())


if __name__ == '__main__':
    main()
