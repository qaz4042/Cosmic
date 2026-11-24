# Rapid combat test overlay

See [feature and acceptance record](../../../../docs/快速战斗-高频连发.md).

The local main branch does not contain every deployed training extension. `source/`
snapshots the three affected deployment classes with this feature's minimal edits.
The generic service lives at `src/main/java/server/TrainingRapidCombat.java`.
`backup/Cosmic.jar` is the exact downloaded base; it is not uploaded or modified.

Run `python3 tools/rapid-combat/build.py` from the Cosmic root to compile against
that base with Java 21 and produce `Cosmic-rapid-test.jar` plus `manifest.json`.
The script verifies that only the three adapted classes and their nested classes,
and the new TrainingRapidCombat classes change. Generated jars/classes and the
backup are ignored by Git. Preserve or reacquire the matching base before a rebuild.

Run the `TrainingRapidCombatTest` Maven test using Java 21, then run
`python3 tools/rapid-combat/check.py` to exercise the actual packaged command and
Character overlays using that test run's dependency classpath. No game server or
network session starts. `check.py` tests mode changes, acknowledgements, invalid
updates, stop paths, map policy and compatibility with deployed pet-v2 state.

The app and test jar are local artifacts. This does not deploy, restart, or replace
the running service. Compare the live jar hash with `manifest.json` before any
later deployment; rebuild against a fresh base if it changed.
