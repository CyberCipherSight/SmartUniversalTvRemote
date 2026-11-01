#!/usr/bin/env python3
"""Workspace-only Linux x86_64 toolchain setup. Downloads are pinned and SHA-256 checked."""
from pathlib import Path
import hashlib
import json
import os
import platform
import shutil
import subprocess
import tarfile
import tempfile
import venv
import zipfile

ROOT = Path(__file__).resolve().parents[2]
TOOLS = ROOT / '.tools/esp32'
DOWNLOADS = TOOLS / 'downloads'


def fetch(record):
    path = DOWNLOADS / record['archive']
    expected = record['sha256']
    if not path.exists() or hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        subprocess.run(['curl', '--location', '--fail', '--retry', '2', '--output', str(path), record['url']], check=True)
    if hashlib.sha256(path.read_bytes()).hexdigest() != expected:
        raise SystemExit('SHA-256 mismatch: ' + str(path))
    return path


def install(record, strip=True):
    archive = fetch(record)
    destination = TOOLS / record['destination']
    if destination.exists() and any(destination.iterdir()):
        return
    destination.parent.mkdir(parents=True, exist_ok=True)
    with tempfile.TemporaryDirectory(dir=TOOLS) as temp:
        if zipfile.is_zipfile(archive):
            with zipfile.ZipFile(archive) as z:
                for info in z.infolist():
                    target = (Path(temp) / info.filename).resolve()
                    if not target.is_relative_to(Path(temp).resolve()): raise SystemExit('Unsafe archive path')
                    z.extract(info, temp)
                    if not info.is_dir(): target.chmod((info.external_attr >> 16) & 0o777 or 0o644)
        else:
            with tarfile.open(archive) as t: t.extractall(temp, filter='data')
        children = list(Path(temp).iterdir())
        if strip:
            if len(children) != 1 or not children[0].is_dir(): raise SystemExit('Unexpected archive layout')
            if destination.exists(): destination.rmdir()
            shutil.move(str(children[0]), str(destination))
        else:
            destination.mkdir(exist_ok=True)
            for child in children: shutil.move(str(child), str(destination / child.name))


def main():
    if platform.system() != 'Linux' or platform.machine() not in ['x86_64', 'AMD64']:
        raise SystemExit('This pinned toolchain is for Linux x86_64. Use Arduino CLI with the same core/library on another host.')
    DOWNLOADS.mkdir(parents=True, exist_ok=True)
    lock = json.loads((ROOT / 'firmware/ir_learner/dependencies.json').read_text())
    install(lock['cli'], strip=False)
    install(lock['core'])
    for record in lock['tools'] + lock['builtins']: install(record)
    install(lock['library'])
    wheel = fetch(lock['pyserial'])
    python = TOOLS / 'python/bin/python'
    if not python.exists(): venv.EnvBuilder(with_pip=True).create(TOOLS / 'python')
    subprocess.run([str(python), '-m', 'pip', '--disable-pip-version-check', 'install', '--no-cache-dir', '--no-index', str(wheel)], check=True)
    data = TOOLS / 'data'
    data.mkdir(exist_ok=True)
    # Indexes enable CLI discovery/registration. Actual installed versions/archives stay pinned above.
    for name, url in [
        ('package_index', 'https://downloads.arduino.cc/packages/package_index.tar.bz2'),
        ('library_index', 'https://downloads.arduino.cc/libraries/library_index.tar.bz2')]:
        if not (data / (name + '.json')).exists():
            path = DOWNLOADS / (name + '.tar.bz2')
            subprocess.run(['curl', '--location', '--fail', '--output', str(path), url], check=True)
            with tarfile.open(path) as t: t.extractall(data, filter='data')
    index = data / 'package_esp32_index.json'
    if not index.exists():
        subprocess.run(['curl', '--location', '--fail', '--output', str(index),
                        'https://espressif.github.io/arduino-esp32/package_esp32_index.json'], check=True)
    (TOOLS / 'arduino-cli.yaml').write_text(
        f'directories:\n  data: {data}\n  downloads: {DOWNLOADS}\n  user: {TOOLS / "sketchbook"}\n'
        'board_manager:\n  additional_urls:\n    - https://espressif.github.io/arduino-esp32/package_esp32_index.json\n')
    print('Local tools ready. No host-wide packages, services, permissions or board firmware were changed.')


if __name__ == '__main__': main()
