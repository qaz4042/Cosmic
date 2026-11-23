# Training assist map transfer

`src/main/java/server/TrainingMapTransfer.java` implements the map-transfer lifecycle,
with the `Actor` adapter tested by `TrainingMapTransferTest`. It snapshots confirmed
in-session state before the old map removes a player, restores protection before
joining the destination, then resumes timers after the client confirms arrival.
Death, logout and the explicit stop command discard pending restoration. Silence
remains a map rule; an unchecked source never disables another destination's rule.

The existing deployed training/pet/vacuum APIs are present in the live Cosmic
source but absent from this local checkout's baseline `Character`. To preserve
those existing changes, `apply.py` applies the small adapter and lifecycle hooks
to a copy of that deployed source, checking every insertion before writing:

```sh
python3 tools/training-map-transfer/apply.py /path/to/copied-cosmic-source
```

Copy `TrainingMapTransfer.java` into that tree and compile the modified Character,
PlayerMapTransitionHandler and AssistCommand with the deployed jar as classpath.
Do not overwrite deployed Character with this checkout's unextended baseline.
See `client/OpenStory/TRAINING_AUTO.md` in the workspace for the verified deployment
and rollback location. No database changes or cross-login settings are involved.

Validation: `mvn -Dtest=TrainingMapTransferTest test` with Java 21 or newer.
