#!/usr/bin/env python3
"""Compare labeled capture sessions, without promoting evidence into verified app profiles."""
import argparse
import json
from pathlib import Path


def read_session(path):
    records = []
    for line in (Path(path) / 'captures.jsonl').read_text().splitlines():
        record = json.loads(line)
        if record['source'].get('function'):
            records.append(record)
    return records


def decoded_key(record):
    frame = record['firmware']
    if frame['protocol'].upper() == 'UNKNOWN' or frame['overflow'] or frame['repeat']:
        return None
    if frame.get('state_hex'):
        return frame['protocol'], frame['bits'], frame['state_hex']
    if frame.get('address_hex') is None or frame.get('command_hex') is None:
        return None
    # Library address/command semantics, not Flipper or IRDB fields. RC toggle bits may vary.
    result = (frame['protocol'], frame['bits'], frame['address_hex'], frame['command_hex'])
    return result if frame['protocol'].upper() in {'RC5', 'RC5X', 'RC6'} else result + (frame.get('value_hex'),)


def raw_matches(a, b):
    left, right = a['firmware']['raw_us'], b['firmware']['raw_us']
    if not left or len(left) != len(right) or a['firmware']['overflow'] or b['firmware']['overflow'] or a['firmware']['repeat'] or b['firmware']['repeat']:
        return False
    return all(x > 0 and y > 0 and abs(x-y) <= max(100, 0.20 * max(x, y)) for x, y in zip(left, right))


def compare(left, right):
    labels = sorted(set(r['source']['function'] for r in left + right))
    report = []
    for label in labels:
        a = [r for r in left if r['source']['function'] == label]
        b = [r for r in right if r['source']['function'] == label]
        decoded_pairs = [(x['capture_id'], y['capture_id']) for x in a for y in b
                         if decoded_key(x) is not None and decoded_key(x) == decoded_key(y)]
        raw_pairs = [(x['capture_id'], y['capture_id']) for x in a for y in b if raw_matches(x, y)]
        report.append({'function': label, 'left_samples': len(a), 'right_samples': len(b),
                       'decoded_matching_capture_pairs': decoded_pairs, 'raw_matching_capture_pairs': raw_pairs,
                       'verification': 'comparison_only', 'speaker_response_verified': False,
                       'carrier_hz_measured': None})
    return {'schema_version': 1, 'comparison': report,
            'warning': 'A matching capture does not verify speaker response, source identity, or carrier frequency. Unknown hashes are not protocol commands. No app profile has been modified.'}


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('left'); parser.add_argument('right'); parser.add_argument('--output')
    args = parser.parse_args()
    report = json.dumps(compare(read_session(args.left), read_session(args.right)), indent=2) + '\n'
    if args.output: Path(args.output).write_text(report)
    else: print(report, end='')
