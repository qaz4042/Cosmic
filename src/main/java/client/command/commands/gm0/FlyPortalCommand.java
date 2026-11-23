package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import server.maps.Portal;
import tools.PacketCreator;
import java.util.Map;
import java.util.WeakHashMap;

/** Virtual flight enters a named portal in the CURRENT map, without moving the character.
 * Only proximity is waived. The real portal still owns destination and script conditions.
 */
public class FlyPortalCommand extends Command {
    private final Map<client.Character, Long> requests = new WeakHashMap<>();

    public FlyPortalCommand() { setDescription("Enter the current map's portal from virtual flight."); }

    synchronized boolean acceptRequest(client.Character player, long now) {
        Long last = requests.get(player);
        if (last != null && now - last < 700_000_000L) return false;
        requests.put(player, now);
        return true;
    }

    static boolean usable(Client c) {
        var p = c.getPlayer();
        return p != null && p.isAlive() && !p.isBanned() && !p.isChangingMaps()
                && p.getMap() != null && p.getTrade() == null && p.getShop() == null
                && p.getPlayerShop() == null && p.getMiniGame() == null
                && p.getHiredMerchant() == null && !p.getCashShop().isOpened() && c.getCM() == null;
    }

    static boolean enterable(Portal portal) {
        if (portal == null || !portal.getPortalStatus()) return false;
        // Spawn points, springs and automatic-touch portals are not UP-key doors.
        return switch (portal.getType()) {
            case 1, 2, 6, 7, 8, 10, 11 -> true;
            default -> false;
        };
    }

    @Override public void execute(Client c, String[] params) {
        var p = c.getPlayer();
        if (p == null) return;
        if (!usable(c)) { p.yellowMessage("当前状态不能使用虚拟飞过门。"); return; }
        int source;
        try {
            if (params.length != 2) throw new NumberFormatException();
            source = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            p.yellowMessage("虚拟飞过门参数无效。"); return;
        }
        if (source != p.getMapId() || !acceptRequest(p, System.nanoTime())) return;
        Portal portal = p.getMap().getPortal(params[1]);
        if (!enterable(portal)) {
            p.yellowMessage("该传送门不可进入或尚未开放。");
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        portal.enterPortal(c);
    }
}
