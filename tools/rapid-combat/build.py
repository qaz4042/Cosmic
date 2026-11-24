#!/usr/bin/env python3
"""Build an overlay against the exact deployed backup; never changes the remote server."""
from pathlib import Path
from zipfile import ZipFile
import subprocess, hashlib, json
root=Path(__file__).resolve().parents[2]
b=root/'tools/rapid-combat'
classes=b/'classes'; classes.mkdir(exist_ok=True)
sources=[root/'src/main/java/server/TrainingRapidCombat.java',*sorted((b/'source').rglob('*.java'))]
java=Path('/Users/zubin/Library/Java/JavaVirtualMachines/temurin-21.0.12.1/Contents/Home/bin')
subprocess.run([str(java/'javac'),'-encoding','UTF-8','--release','21','-cp',str(b/'backup/Cosmic.jar'),
                '-sourcepath',str(b/'empty-sourcepath'),'-d',str(classes),*[str(p) for p in sources]],check=True)
updates={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
def allowed(name):
    return name in ('client/Character.class','client/command/commands/gm0/AssistCommand.class',
                    'net/server/channel/handlers/AbstractDealDamageHandler.class') or any(name.startswith(prefix) for prefix in (
                        'client/Character$','net/server/channel/handlers/AbstractDealDamageHandler$',
                        'server/TrainingRapidCombat'))
assert all(allowed(n) for n in updates), sorted(updates)
output=b/'Cosmic-rapid-test.jar'
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(output,'w') as new:
    for info in old.infolist(): new.writestr(info,updates.pop(info.filename,old.read(info.filename)))
    for name,data in updates.items(): new.writestr(name,data)
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(output) as new:
    assert not new.testzip()
    changed=[n for n in new.namelist() if n not in old.namelist() or new.read(n)!=old.read(n)]
    assert all(allowed(n) for n in changed),changed
manifest={'base_sha256':hashlib.sha256((b/'backup/Cosmic.jar').read_bytes()).hexdigest(),
          'output_sha256':hashlib.sha256(output.read_bytes()).hexdigest(),'changed_classes':changed}
(b/'manifest.json').write_text(json.dumps(manifest,indent=2)+'\n')
print('Verified overlay:',output)
print('Changed classes:',', '.join(changed))
