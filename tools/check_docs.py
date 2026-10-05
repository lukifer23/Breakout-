#!/usr/bin/env python3
"""Keep current Markdown links/version/commands aligned; archives retain history."""
from pathlib import Path
import re,sys
root=Path(__file__).resolve().parents[1];errors=[]
files=[root/'README.md']+list((root/'Docs').glob('*.md'))+list((root/'ios').glob('*.md'))+[root/'.github/README.md']
for path in files:
    content=path.read_text()
    for target in re.findall(r'\]\(([^)]+)\)',content):
        if '://' in target or target.startswith('#'):continue
        target=target.split('#')[0].split(' "')[0].strip('<>')
        if target and not (path.parent/target).exists():errors.append(f'{path.relative_to(root)}: missing {target}')
    if path.name not in ['RELEASE_NOTES.md','MODERNIZATION_BASELINE.md','TOOLING_SECURITY.md']:
        if re.search(r'CI=true\s+\./gradlew',content):errors.append(f'{path.name}: unsafe old release fallback command')
        if re.search(r'(targetSdk|SDK platform) 35',content):errors.append(f'{path.name}: stale API level')
version=dict(line.split('=',1) for line in (root/'version.properties').read_text().splitlines() if '=' in line and not line.startswith('#'))
for name in ['README.md','Docs/BUILD.md','Docs/RELEASE_CHECKLIST.md']:
    if version['versionName'] not in (root/name).read_text():errors.append(f'{name}: missing authoritative release version')
if errors:print('\n'.join(errors),file=sys.stderr);sys.exit(1)
print(f'Current documentation check passed ({len(files)} documents)')
