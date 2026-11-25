from pathlib import Path
from zipfile import ZipFile
b=Path('/opt/maple-poc/job-switch-compact-20261124')
p=Path('/opt/maple-poc/cosmic/target/job-switch-compact/classes/server/AdventurerJobSwitch.class')
name='server/AdventurerJobSwitch.class'
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(b/'Cosmic.next.jar','w') as new:
 for info in old.infolist():new.writestr(info,p.read_bytes() if info.filename==name else old.read(info.filename))
with ZipFile(b/'backup/Cosmic.jar') as old, ZipFile(b/'Cosmic.next.jar') as new:
 changed=[n for n in new.namelist() if old.read(n)!=new.read(n)]
 assert changed==[name], changed
 print('Verified: only AdventurerJobSwitch.class changed')
