#!/usr/bin/env python3
"""Build, identify and flash the receive-only learner. Flash is guarded by chip identification and backup."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import subprocess
import sys
from datetime import datetime, timezone

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / '.tools/esp32'
SKETCH = ROOT / 'firmware/ir_learner'
BUILD = SKETCH / 'build'
PYTHON = TOOLS / ('windows-python/Scripts/python.exe' if os.name == 'nt' else 'python/bin/python')
ESPTOOL = TOOLS / 'data/packages/esp32/tools/esptool_py/4.5.1/esptool.py'
SAFE_CLASSIC_PINS = {13, 14, 18, 19, 21, 22, 23, 25, 26, 27, 32, 33}


def environment():
    env = dict(os.environ)
    env['PATH'] = str(PYTHON.parent) + os.pathsep + env.get('PATH', '')
    env['PYTHONPYCACHEPREFIX'] = str(TOOLS / 'pycache')
    return env


def run(command, timeout=900):
    subprocess.run(list(map(str, command)), cwd=ROOT, env=environment(), check=True, timeout=timeout)


def esptool(port, *arguments):
    launcher = [PYTHON, '-m', 'esptool'] if os.name == 'nt' else [PYTHON, ESPTOOL]
    command = [*launcher, '--port', port, '--baud', '460800', *arguments]
    result = subprocess.run(list(map(str, command)), cwd=ROOT, env=environment(),
                            text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, timeout=180)
    if result.returncode:
        raise SystemExit('ESP32 serial operation failed (no firmware written unless a write was explicitly in progress):\n' + result.stdout)
    return result.stdout


def resolve_port(requested):
    if requested != 'auto':
        if os.name == 'nt':
            from serial.tools import list_ports
            if requested.upper() not in {p.device.upper() for p in list_ports.comports()}:
                raise SystemExit(f'Serial device is not accessible: {requested}')
        elif not Path(requested).exists(): raise SystemExit(f'Serial device is not accessible: {requested}')
        return requested
    result = subprocess.run([str(PYTHON), str(ROOT / 'tools/ir_capture/record.py'), 'ports'],
                            text=True, capture_output=True, check=True)
    records = json.loads(result.stdout)
    known = [p for p in records if p['known_bridge']]
    if len(known) != 1:
        raise SystemExit('A single ESP32-compatible serial port is required. Detected: ' + json.dumps(records))
    return known[0]['port']


def build(gpio):
    if gpio not in SAFE_CLASSIC_PINS:
        raise SystemExit('Choose a safe input GPIO for the identified classic ESP32; flash/strap/USB pins are not automatically used.')
    flags = (SKETCH / 'receive_only_flags.txt').read_text().strip()
    manifest_path = BUILD / 'build-manifest.json'
    if manifest_path.exists():
        previous = json.loads(manifest_path.read_text())
        artifacts = previous.get('artifacts_sha256', {})
        if (previous.get('source_sha256') == hashlib.sha256((SKETCH / 'ir_learner.ino').read_bytes()).hexdigest()
                and previous.get('gpio') == gpio and previous.get('send_features_disabled') == flags
                and len(artifacts) >= 4 and all((BUILD / name).exists() and hashlib.sha256((BUILD / name).read_bytes()).hexdigest() == digest
                                              for name, digest in artifacts.items())):
            print('Reusing the verified receive-only firmware build.')
            return
    if os.name == 'nt':
        raise SystemExit('A matching verified firmware build is required on Windows; rebuild with the documented Linux toolchain first.')
    run([TOOLS / 'bin/arduino-cli', '--config-file', TOOLS / 'arduino-cli.yaml', 'compile',
         '--fqbn', 'esp32:esp32:esp32', '--jobs', '4', '--build-path', BUILD,
         '--build-property', f'compiler.cpp.extra_flags={flags} -DIR_CAPTURE_GPIO={gpio}',
         '--build-property', 'runtime.tools.ctags.path=' + str(TOOLS / 'data/packages/builtin/tools/ctags/5.8-arduino11'), SKETCH])
    artifacts = {p.name: hashlib.sha256(p.read_bytes()).hexdigest() for p in BUILD.glob('ir_learner.ino.*')
                 if p.suffix in ['.bin', '.elf']}
    (BUILD / 'build-manifest.json').write_text(json.dumps({
        'receive_only': True, 'fqbn': 'esp32:esp32:esp32', 'gpio': gpio,
        'core': '2.0.17', 'decoder': 'IRremoteESP8266-2.9.0',
        'source_sha256': hashlib.sha256((SKETCH / 'ir_learner.ino').read_bytes()).hexdigest(),
        'send_features_disabled': flags, 'artifacts_sha256': artifacts,
    }, indent=2) + '\n')


def identify(port):
    identity = esptool(port, 'chip_id')
    match = re.search(r'Chip is (ESP32[^\r\n]*)', identity)
    if not match or re.match(r'ESP32[- ](?:S\d|C\d|H\d|P\d)', match.group(1)):
        raise SystemExit('This build is for classic ESP32 only. Identify and build for the actual board before flashing.\n' + identity)
    return identity, match.group(1)


def flash(args):
    port = resolve_port(args.port)
    build(args.gpio)
    identity, chip = identify(port)
    flash_info = esptool(port, 'flash_id')
    size = re.search(r'Detected flash size:\s*(\d+)\s*([MK])B', flash_info)
    if not size: raise SystemExit('Cannot determine flash size; refusing an unbacked-up upload.')
    flash_bytes = int(size.group(1)) * (1024**2 if size.group(2) == 'M' else 1024)
    backup = ROOT / 'captures/ir/backups' / datetime.now(timezone.utc).strftime('%Y%m%dT%H%M%S%fZ')
    backup.mkdir(parents=True, exist_ok=False)
    (backup / 'identity.txt').write_text(identity + '\n' + flash_info)
    backup_file = backup / 'original-flash.bin'
    esptool(port, 'read_flash', '0', str(flash_bytes), str(backup_file))
    if backup_file.stat().st_size != flash_bytes: raise SystemExit('Incomplete backup; upload aborted.')
    (backup / 'backup.json').write_text(json.dumps({'chip': chip, 'port': port, 'bytes': flash_bytes,
        'sha256': hashlib.sha256(backup_file.read_bytes()).hexdigest()}, indent=2) + '\n')
    output = esptool(port, '--chip', 'esp32', '--before', 'default_reset', '--after', 'hard_reset',
                     'write_flash', '-z', '--flash_mode', 'dio', '--flash_freq', '40m', '--flash_size', 'detect',
                     '0x1000', str(BUILD / 'ir_learner.ino.bootloader.bin'),
                     '0x8000', str(BUILD / 'ir_learner.ino.partitions.bin'),
                     '0xe000', str(TOOLS / 'data/packages/esp32/hardware/esp32/2.0.17/tools/partitions/boot_app0.bin'),
                     '0x10000', str(BUILD / 'ir_learner.ino.bin'))
    (backup / 'upload.log').write_text(output)
    print(f'Uploaded receive-only firmware on GPIO {args.gpio}; original flash backed up at {backup}.', flush=True)
    print('Starting the local recorder. No IR transmission is enabled.', flush=True)
    run([PYTHON, ROOT / 'tools/ir_capture/record.py', 'listen', '--port', port, '--source', 'phone_app'], timeout=None)


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('action', choices=['build', 'probe', 'flash'])
    parser.add_argument('--gpio', type=int, default=27)
    parser.add_argument('--port', default='auto')
    args = parser.parse_args()
    if args.action == 'build': build(args.gpio)
    elif args.action == 'probe':
        identity, chip = identify(resolve_port(args.port)); print(chip)
    else: flash(args)
