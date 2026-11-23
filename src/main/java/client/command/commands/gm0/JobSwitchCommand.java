package client.command.commands.gm0;

import client.Client;
import client.command.Command;

public class JobSwitchCommand extends Command {
    public JobSwitchCommand() {
        setDescription("冒险家同系二转职业切换（30–69级，需卸下装备）。");
    }

    @Override
    public void execute(Client client, String[] params) {
        if (client.getCM() != null) return;
        client.getAbstractPlayerInteraction().openNpc(1022000, "job_switch");
    }
}
