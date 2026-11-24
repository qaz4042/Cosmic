package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class JobSwitchCommand extends Command {
    public JobSwitchCommand() {
        setDescription("冒险家同系职业分支切换（30级以上，保持转数，需卸下装备）。");
    }

    @Override
    public void execute(Client client, String[] params) {
        var player = client.getPlayer();
        if (player == null) return;
        if (params.length == 1 && "status".equals(params[0])) {
            player.sendPacket(tools.PacketCreator.serverNotice(5, "[JobSwitch:state:v1] "
                    + player.getMapId() + " " + player.getJob().getId() + " "
                    + server.AdventurerJobSwitch.status(player) + " " + player.getJobSwitchQuietSeconds()));
            return;
        }
        if (client.getCM() != null) return;
        client.getAbstractPlayerInteraction().openNpc(1022000, "job_switch");
    }
}
