from pathlib import Path
from zipfile import ZipFile
import hashlib
base=Path('/opt/maple-poc/ammo-shop-20261124')
jar=Path('/opt/maple-poc/cosmic/target/Cosmic.jar')
backup=base/'backup/Cosmic.jar'
assert hashlib.sha256(jar.read_bytes()).digest()==hashlib.sha256(backup.read_bytes()).digest(), 'Live jar changed; rebase required'
name='server/Shop.class'
compiled=Path('/opt/maple-poc/cosmic/target/ammo-shop/classes/server/Shop.class').read_bytes()
with ZipFile(backup) as old, ZipFile(base/'Cosmic.next.jar','w') as new:
    for info in old.infolist():
        new.writestr(info, compiled if info.filename==name else old.read(info.filename))
with ZipFile(backup) as old, ZipFile(base/'Cosmic.next.jar') as new:
    assert [n for n in new.namelist() if old.read(n)!=new.read(n)]==[name]
print('Verified only server/Shop.class changed')
(base/'Cosmic.next.jar').replace(jar)
