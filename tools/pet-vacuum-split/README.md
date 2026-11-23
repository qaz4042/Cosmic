# Separate assisted pet pickup filters

The deployed training overlays are absent from the local Cosmic baseline. Files
under `source/` snapshot only the two affected deployed training classes; keep
unrelated deployment changes when applying future revisions. `apply.py` patches
a copy of deployed `client/Character.java`; never replace it with the local
unextended Character. `package.py` updates only Character classes, AssistCommand,
and TrainingPetVacuum in a backup jar and verifies the changed class set.

`@assist petvac mesos on/off` and `petvac items on/off` independently modify a
server mask (bit 0: mesos, bit 1: items). The v2 state notice contains map ID and
mask 0..3; old `petvac on/off` remains compatible and toggles both. Last filters
survive the existing in-session map-transfer timer restart; explicit stop disables
both. Ordinary client pet pickup preferences are independent of these assists.

Java 21 validation against the deployed jar: compile the three changed sources,
then compile/run CheckPetFilter with those classes first on the classpath. It
checks all four server item/meso combinations. Client production panel regression
is `client/OpenStory/tests/training-panel.py`, including v2 parsing, commands,
independent checkboxes, malformed states and old-server gating.

Deployment/rollback snapshot: `/opt/maple-poc/pet-split-20261124/backup/` on my4g.
