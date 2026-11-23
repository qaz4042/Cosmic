from pathlib import Path
from zipfile import ZipFile
import sys
b=Path(sys.argv[1])
updates={p.relative_to(b/'classes').as_posix():p.read_bytes() for p in (b/'classes').rglob('*.class')}
assert all(n.startswith('client/Character') or n in ('server/TrainingPetVacuum.class','client/command/commands/gm0/AssistCommand.class') for n in updates)
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(b/'Cosmic.next.jar','w') as new:
    for info in old.infolist(): new.writestr(info,updates.pop(info.filename,old.read(info.filename)))
    for name,data in updates.items(): new.writestr(name,data)
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(b/'Cosmic.next.jar') as new:
    changed=[n for n in new.namelist() if n not in old.namelist() or new.read(n)!=old.read(n)]
    assert all(n.startswith('client/Character') or n in ('server/TrainingPetVacuum.class','client/command/commands/gm0/AssistCommand.class') for n in changed)
    print('Verified changed classes:',changed)
