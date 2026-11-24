from pathlib import Path
from zipfile import ZipFile
import re
b=Path('/opt/maple-poc/job-switch-unequip-20261124')
classes=Path('/opt/maple-poc/cosmic/target/job-switch-unequip-classes')
roots=['client/Character','server/AdventurerJobSwitch','server/JobSwitchEquipment','constants/string/CharsetConstants']
def allowed(n):return any(n==r+'.class' or n.startswith(r+'$') for r in roots)
updates={p.relative_to(classes).as_posix():p.read_bytes() for p in classes.rglob('*.class')}
assert all(allowed(n) for n in updates)
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(b/'Cosmic.next.jar','w') as new:
 for info in old.infolist():new.writestr(info,updates.pop(info.filename,old.read(info.filename)))
 for n,data in updates.items():new.writestr(n,data)
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(b/'Cosmic.next.jar') as new:
 changed=[n for n in new.namelist() if n not in old.namelist() or new.read(n)!=old.read(n)]
 assert all(allowed(n) for n in changed)
 assert not(set(old.namelist())-set(new.namelist()))
 for n in ['server/TrainingPetVacuum.class','client/command/commands/gm0/AssistCommand.class','server/ShopInventoryActions.class']:
  assert new.read(n)==old.read(n)
 print('Verified changed classes:',len(changed))
config=(b/'backup/config.yaml').read_text()
config,count=re.subn(r'(?m)^(\s*CHARSET:)\s*US-ASCII',r'\1 UTF-8',config)
assert count==1
(b/'config.next.yaml').write_text(config)
(b/'config.next.yaml').chmod(0o600)
