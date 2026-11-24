from pathlib import Path
from zipfile import ZipFile
base = Path('/opt/maple-poc/job-switch-20261124-brief')
classes = Path('/opt/maple-poc/cosmic/target/job-switch-compile')
roots = ['client/Character', 'client/command/CommandsExecutor', 'client/command/commands/gm0/JobSwitchCommand',
         'server/AdventurerJobSwitch', 'server/JobSwitchQuietPeriod']
roots += ['net/server/channel/handlers/' + name for name in ['AbstractDealDamageHandler','SpecialMoveHandler','TakeDamageHandler','PlayerLoggedinHandler']]
def allowed(name):
    return any(name == root + '.class' or name.startswith(root + '$') for root in roots)
updates = {p.relative_to(classes).as_posix(): p.read_bytes() for p in classes.rglob('*.class')}
assert updates and all(allowed(n) for n in updates)
with ZipFile(base/'backup/Cosmic.jar') as old, ZipFile(base/'Cosmic.next.jar','w') as new:
    for info in old.infolist():
        new.writestr(info, updates.pop(info.filename, old.read(info.filename)))
    for name, data in updates.items(): new.writestr(name, data)
with ZipFile(base/'backup/Cosmic.jar') as old, ZipFile(base/'Cosmic.next.jar') as new:
    oldnames = set(old.namelist())
    changed = [n for n in new.namelist() if n not in oldnames or old.read(n) != new.read(n)]
    assert all(allowed(n) for n in changed)
    assert not (oldnames - set(new.namelist()))
    for preserved in ['server/TrainingPetVacuum.class','client/command/commands/gm0/AssistCommand.class','server/ShopInventoryActions.class']:
        assert old.read(preserved) == new.read(preserved)
    print('Verified changed classes:',len(changed))
    print('Preserved training, pet-vacuum and shop classes byte-for-byte.')
