#!/usr/bin/env python3
"""Apply the small hooks to a Cosmic tree that already has the deployed training overlay.
All inputs are checked before any file is written; repeat application is rejected.
"""
from pathlib import Path
import sys
root=Path(sys.argv[1]).resolve()
changes={}
def patch(path, edits):
    file=root/path
    source=file.read_text()
    for before,after in edits:
        if source.count(before)!=1:
            raise SystemExit(f'Expected exactly one hook in {file}: {before[:90]}')
        source=source.replace(before,after,1)
    changes[file]=source
patch('src/main/java/client/Character.java',[
 ('public class Character extends AbstractCharacterObject {','public class Character extends AbstractCharacterObject implements server.TrainingMapTransfer.Actor {'),
 ('    private volatile server.TrainingPetVacuum trainingPetVacuum;', '''    public Object getTrainingMapKey() { return getMap(); }
    public boolean isTrainingMapAllowed() { return server.TrainingAssist.isAllowed(getMap()); }
    public boolean isTrainingSilenceEnabled() { return getMap() != null && getMap().isTrainingMonsterSilenced(); }
    public void setTrainingSilenceEnabled(boolean enabled) { getMap().setTrainingMonsterSilenced(enabled); }

    private volatile server.TrainingPetVacuum trainingPetVacuum;'''),
 ('        this.mapTransitioning.set(true);','        server.TrainingMapTransfer.prepare(this, to);\n        this.mapTransitioning.set(true);'),
 ('            map = to;\n            setPosition(pos);\n            map.addPlayer(this);','            map = to;\n            setPosition(pos);\n            server.TrainingMapTransfer.enter(this);\n            map.addPlayer(this);'),
 ('    private void hpChangeAction(int oldHp) {\n        if (hp <= 0) {','    private void hpChangeAction(int oldHp) {\n        if (hp <= 0) {\n            server.TrainingMapTransfer.cancel(this);'),
 ('    public final void empty(final boolean remove) {','    public final void empty(final boolean remove) {\n        server.TrainingMapTransfer.cancel(this);')])
patch('src/main/java/net/server/channel/handlers/PlayerMapTransitionHandler.java',[
 ('        chr.setMapTransitionComplete();','''        chr.setMapTransitionComplete();
        if (server.TrainingMapTransfer.complete(chr)) {
            chr.sendPacket(PacketCreator.serverNotice(5, "[TrainingAssist:auto:v1] "
                    + chr.getMapId() + " " + chr.getTrainingAutoSkill()));
        }
        new client.command.commands.gm0.AssistCommand().execute(c, new String[]{"sync"});''')])
patch('src/main/java/client/command/commands/gm0/AssistCommand.java',[
 ('        if (params.length == 1 && params[0].equals("off")) {','        if (params.length == 1 && params[0].equals("off")) {\n            server.TrainingMapTransfer.cancel(player);')])
for file,source in changes.items(): file.write_text(source)
print('Applied Character, map-arrival and stop-command hooks.')
