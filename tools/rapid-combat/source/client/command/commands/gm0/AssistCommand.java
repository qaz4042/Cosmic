package client.command.commands.gm0;

import client.Character;
import client.Client;
import client.command.Command;
import server.TrainingAssist;
import server.maps.MapleMap;
import tools.PacketCreator;

public class AssistCommand extends Command {
    private final java.util.Map<Character,String> rotations = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    {
        setDescription("Training assists: status, invincible on/off, silence on/off, vac on/off, auto on [skillId]/off, off.");
    }

    @Override
    public void execute(Client client, String[] params) {
        Character player = client.getPlayer();
        try {
            executeRequest(player, params);
        } finally {
            sendState(player);
        }
    }

    private void executeRequest(Character player, String[] params) {
        MapleMap map = player.getMap();
        if (params.length == 1 && params[0].equals("sync")) return;
        if (params.length > 0 && params[0].equals("rapid")) {
            if (params.length == 2 && params[1].equals("off")) {
                player.setTrainingAutoSkill(-1); rotations.remove(player); sendAutoState(player, -1); return;
            }
            try {
                if (params.length != 4 || !params[1].equals("on") || !TrainingAssist.isAllowed(map))
                    throw new IllegalArgumentException();
                int rate = Integer.parseInt(params[2]), skill = Integer.parseInt(params[3]);
                if (!server.TrainingRapidCombat.validRate(rate) || !server.TrainingRapidCombat.supported(skill)
                        || !player.isAlive() || player.getSkillLevel(skill) <= 0
                        || !player.isLoggedin() || player.isChangingMaps()) throw new IllegalArgumentException();
                if (!server.TrainingRapidCombat.active(player) || player.getTrainingAutoSkill() != skill) {
                    player.setTrainingAutoSkill(skill); rotations.remove(player); sendAutoState(player, skill);
                }
                server.TrainingRapidCombat.start(player, skill, rate);
                player.yellowMessage("高频连发："+rate+"次施放/秒；逐次消耗MP/弹药，F8可停止。实验档尚待联机验证。");
            } catch (IllegalArgumentException ex) {
                player.yellowMessage("用法：@assist rapid on 10/20/40 技能ID | rapid off；仅支持已学海盗一转三个攻击技能。");
            }
            return;
        }
        if (params.length>0 && params[0].equals("rotation")) {
            if (!player.isAlive() || !TrainingAssist.isAllowed(map)) { player.yellowMessage("当前状态不能开启挂机。"); return; }
            try {
                String plan=server.TrainingSkillSupport.validate(player,params);
                int first=Integer.parseInt(plan.substring(0,plan.indexOf(':')));
                player.setTrainingAutoSkill(first); rotations.put(player,plan);
                player.yellowMessage("技能轮换已开启；手动操作不会关闭，F8 可停止。");
            } catch (IllegalArgumentException ex) { player.yellowMessage("轮换无效：最多4个已学支持技能，间隔0.2～600秒，不能重复。"); }
            return;
        }
        if (params.length == 3 && params[0].equals("petvac")
                && (params[1].equals("mesos") || params[1].equals("items"))
                && (params[2].equals("on") || params[2].equals("off"))) {
            int bit = params[1].equals("mesos") ? 1 : 2;
            int mask = player.getTrainingPetVacuumMask();
            mask = params[2].equals("on") ? mask | bit : mask & ~bit;
            if (!player.setTrainingPetVacuumMask(mask))
                player.yellowMessage("宠吸需要在已开放地图中存活并召唤宠物。");
            return;
        }
        if (params.length == 2 && params[0].equals("petvac")
                && (params[1].equals("on") || params[1].equals("off"))) {
            if (!player.setTrainingPetVacuumMask(params[1].equals("on") ? 3 : 0))
                player.yellowMessage("宠吸需要在已开放地图中存活并召唤宠物。");
            return;
        }
        if (params.length == 4 && params[0].equals("vac") && params[1].equals("settings")) {
            try {
                player.configureTrainingVacuum(Integer.parseInt(params[2]), Integer.parseInt(params[3]));
            } catch (IllegalArgumentException ex) {
                player.yellowMessage("Vacuum uses the whole map with no count cap.");
            }
            return;
        }
        if (params.length == 0 || (params.length == 1 && params[0].equals("status"))) {
            player.yellowMessage("Training map: " + TrainingAssist.isAllowed(map)
                    + "; invincible (you): " + player.isTrainingInvulnerable()
                    + "; vacuum (you): " + player.isTrainingVacuumActive()
                    + "; auto skill (you, -1=off): " + player.getTrainingAutoSkill()
                    + "; monster silence (whole map, including bosses): "
                    + (map != null && map.isTrainingMonsterSilenced()));
            return;
        }
        if (params.length == 1 && params[0].equals("off")) {
            player.setTrainingInvulnerable(false);
            player.clearTrainingActivity();
            sendAutoState(player, -1);
            if (map != null && map.isTrainingMonsterSilenced()) setSilence(player, map, false);
            player.yellowMessage("Your training activities and this map's monster silence are OFF.");
            return;
        }
        if (params[0].equals("auto")) {
            handleAuto(player, params);
            return;
        }
        if (params.length != 2 || !(params[0].equals("invincible") || params[0].equals("silence") || params[0].equals("vac"))
                || !(params[1].equals("on") || params[1].equals("off"))) {
            player.yellowMessage("Syntax: @assist status | invincible on/off | silence on/off | vac on/off | auto on [skillId]/off | off");
            return;
        }
        boolean enabled = params[1].equals("on");
        if (enabled && (!TrainingAssist.isAllowed(map) || !player.isAlive())) {
            player.yellowMessage("Enable assists only while alive in a configured training map.");
            return;
        }
        if (params[0].equals("invincible")) {
            player.setTrainingInvulnerable(enabled);
            player.yellowMessage("Invincible (you): " + player.isTrainingInvulnerable());
        } else if (params[0].equals("vac")) {
            if (enabled) {
                if (!player.startTrainingVacuum()) {
                    player.yellowMessage("Cannot start vacuum: no valid nearby floor, another player owns it, or the session is unavailable.");
                    return;
                }
            } else {
                player.stopTrainingVacuum();
            }
            player.yellowMessage("Vacuum (you, ordinary mobile monsters only): " + player.isTrainingVacuumActive());
        } else if (map != null) {
            setSilence(player, map, enabled);
        }
    }

    private void sendState(Character player) {
        MapleMap map = player.getMap();
        if (map == null) return;
        player.sendPacket(PacketCreator.serverNotice(5, "[TrainingAssist:state:v1] "
                + map.getId() + " " + (TrainingAssist.isAllowed(map) ? 1 : 0)
                + " " + (player.isTrainingInvulnerable() ? 1 : 0)
                + " " + (map.isTrainingMonsterSilenced() ? 1 : 0)
                + " " + (player.isTrainingVacuumActive() ? 1 : 0)
                + " " + player.getTrainingAutoSkill()
                + " " + player.getTrainingVacuumRadius() + " " + player.getTrainingVacuumLimit()));
        player.sendPacket(PacketCreator.serverNotice(5, "[TrainingAssist:petvac:v1] "
                + map.getId() + " " + (player.isTrainingPetVacuumActive() ? 1 : 0)));
        player.sendPacket(PacketCreator.serverNotice(5, "[TrainingAssist:petvac:v2] "
                + map.getId() + " " + player.getTrainingPetVacuumMask()));
        if (player.getTrainingAutoSkill()<0) { rotations.remove(player); server.TrainingRapidCombat.stop(player); }
        String plan=rotations.get(player);
        if (plan!=null) player.sendPacket(PacketCreator.serverNotice(5,
                "[TrainingAssist:rotation:v1] "+map.getId()+" "+plan));
        server.TrainingRapidCombat.sendState(player);
        TrainingAssist.sendVacuumCount(player);
        // Authoritative pin snapshots also cover viewers entering an active map.
        // Do not infer the anchor from a moving player's current position.
        for (server.life.Monster monster : map.getAllMonsters()) {
            if (monster.isTrainingPositionPinned()) player.sendPacket(monster.trainingPinPacket(true));
        }
    }

    private void handleAuto(Character player, String[] params) {
        rotations.remove(player);
        if (params.length == 2 && params[1].equals("off")) {
            player.setTrainingAutoSkill(-1);
            sendAutoState(player, -1);
            return;
        }
        if ((params.length != 2 && params.length != 3) || !params[1].equals("on")) {
            player.yellowMessage("Syntax: @assist auto on [skillId] | auto off");
            return;
        }
        int skill;
        try {
            skill = params.length == 3 ? Integer.parseInt(params[2]) : 0;
        } catch (NumberFormatException ex) {
            player.yellowMessage("Invalid skill ID.");
            return;
        }
        // First closed loop: normal attack + the three verified pirate first-job attacks.
        if (!server.TrainingSkillSupport.supported(skill)
                || (skill != 0 && player.getSkillLevel(skill) <= 0)) {
            player.yellowMessage("Supported: 0 (normal), learned 5001001 / 5001002 / 5001003.");
            return;
        }
        if (!player.isAlive() || !TrainingAssist.isAllowed(player.getMap())) {
            player.yellowMessage("Enable assists only while alive in a configured training map.");
            return;
        }
        player.setTrainingAutoSkill(skill);
        sendAutoState(player, skill);
        player.yellowMessage("自动挂机已开启，可在 F8 面板取消勾选或全部停止。");
    }

    private void sendAutoState(Character player, int skill) {
        // Versioned private server notice, consumed only by the matching OpenStory build.
        player.sendPacket(PacketCreator.serverNotice(5, "[TrainingAssist:auto:v1] "
                + player.getMapId() + " " + skill));
    }

    private void setSilence(Character player, MapleMap map, boolean enabled) {
        map.setTrainingMonsterSilenced(enabled);
        map.broadcastMessage(PacketCreator.serverNotice(5, player.getName()
                + ": training monster silence (whole map, including bosses): "
                + map.isTrainingMonsterSilenced()));
    }
}
