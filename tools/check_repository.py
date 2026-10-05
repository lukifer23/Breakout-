#!/usr/bin/env python3
"""Fail if reproducible build products return to the source index."""
from pathlib import PurePosixPath
import subprocess
import sys

files = subprocess.check_output(["git", "ls-files", "-z"]).decode().split("\0")
errors = []
for name in filter(None, files):
    path = PurePosixPath(name)
    if any(part in {".build", "build", "DerivedData"} for part in path.parts):
        errors.append(name)
    elif path.suffix in {".apk", ".aab", ".ipa"} or name == "ios/test_ios_binary" or ".pbxproj.backup" in name:
        errors.append(name)
if errors:
    print("Tracked generated products:\n" + "\n".join(errors), file=sys.stderr)
    sys.exit(1)
print("Repository artifact check passed")
