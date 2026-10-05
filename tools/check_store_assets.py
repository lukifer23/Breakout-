#!/usr/bin/env python3
"""Validate active listing images. Missing captures are allowed only in source CI."""
import argparse, hashlib, pathlib, struct, sys, zlib
p=argparse.ArgumentParser();p.add_argument('--allow-missing',action='store_true');a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]
images=root/'fastlane/metadata/android/en-US/images'
expected=['title','mode_select','gameplay','specialist','powerup','daily_challenges','scoreboard','settings']
errors=[];missing=[]
def png(path):
    b=path.read_bytes()
    if not b.startswith(b'\x89PNG\r\n\x1a\n'): raise ValueError('empty or invalid PNG')
    at=8;size=None;compressed=[];ended=False
    while at<len(b):
        if at+12>len(b): raise ValueError('truncated PNG')
        n=struct.unpack('>I',b[at:at+4])[0];kind=b[at+4:at+8];data=b[at+8:at+8+n]
        if len(data)!=n or at+12+n>len(b): raise ValueError('truncated chunk')
        if zlib.crc32(kind+data)&0xffffffff!=struct.unpack('>I',b[at+8+n:at+12+n])[0]: raise ValueError('bad PNG CRC')
        if kind==b'IHDR': size=struct.unpack('>II',data[:8])
        if kind==b'IDAT': compressed.append(data)
        at+=12+n
        if kind==b'IEND': ended=True;break
    if not size or not ended or at!=len(b) or not compressed: raise ValueError('incomplete PNG')
    if not all(320<=x<=3840 for x in size): raise ValueError(f'invalid dimensions {size}')
    decoder=zlib.decompressobj();pixels=decoder.decompress(b''.join(compressed),100_000_001)
    if len(pixels)>100_000_000 or not decoder.eof or not pixels: raise ValueError('invalid compressed pixels')
    return size,hashlib.sha256(b).hexdigest()
for folder in ['phoneScreenshots','tenInchScreenshots']:
    seen={};directory=images/folder
    for name in expected:
        if not (directory/f'{name}.png').is_file():missing.append(f'{folder}/{name}.png')
    for path in sorted(directory.glob('*.png')):
        try:
            size,digest=png(path)
            if digest in seen:errors.append(f'{path}: duplicates {seen[digest]}')
            seen[digest]=path.name
            if folder=='phoneScreenshots' and size[0]>=size[1]:errors.append(f'{path}: phone capture must be portrait')
            if path.stem not in expected:errors.append(f'{path}: unexpected screenshot name')
        except (ValueError,struct.error,zlib.error) as e: errors.append(f'{path}: {e}')
if errors or (missing and not a.allow_missing):
    print('\n'.join(errors+(['Missing: '+', '.join(missing)] if missing else [])),file=sys.stderr);sys.exit(1)
print('Existing store images valid; capture set INCOMPLETE (publication blocked)' if missing else 'Store capture set valid')
