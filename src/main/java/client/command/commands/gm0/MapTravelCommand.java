package client.command.commands.gm0;

import client.Client;
import client.command.Command;
import constants.id.MapId;
import provider.*;
import provider.wz.WZFiles;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import server.maps.MiniDungeonInfo;
import server.maps.Portal;

import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.WeakHashMap;

/** World-map travel uses the server's own v83 WorldMap mapNo allowlist. */
public class MapTravelCommand extends Command {
    private volatile Set<Integer> destinations;
    private final Map<client.Character, Long> requests = new WeakHashMap<>();

    // Per-session, bounded by live character objects; no persistent cooldown data.
    synchronized boolean acceptRequest(client.Character player, long now) {
        Long previous = requests.get(player);
        if (previous != null && now - previous < 3_000_000_000L) return false;
        requests.put(player, now);
        return true;
    }

    public MapTravelCommand() {
        setDescription("Travel to a regular world-map destination.");
    }

    private synchronized Set<Integer> destinations() {
        if (destinations != null) return destinations;
        Set<Integer> ids = new HashSet<>();
        DataProvider source = DataProviderFactory.getDataProvider(WZFiles.MAP);
        DataDirectoryEntry directory = (DataDirectoryEntry) source.getRoot().getEntry("WorldMap");
        if (directory == null) return Set.of();
        for (DataFileEntry file : directory.getFiles()) {
            Data data = source.getData("WorldMap/" + file.getName());
            if (data == null) continue;
            Data list = data.getChildByPath("MapList");
            if (list == null) continue;
            for (Data spot : list) {
                Data maps = spot.getChildByPath("mapNo");
                if (maps != null) for (Data map : maps) {
                    try {
                        ids.add(Integer.parseInt(String.valueOf(map.getData())));
                    } catch (NumberFormatException ignored) {
                        // Some overview nodes contain world-map links rather than map IDs.
                    }
                }
            }
        }
        destinations = Set.copyOf(ids);
        return destinations;
    }

    static boolean regular(MapleMap map) {
        return map != null && map.getId() >= 0 && map.getId() < 900000000
                && map.getId() != MapId.GM_MAP && map.getId() != MapId.JAIL
                && map.getEventInstance() == null && !MiniDungeonInfo.isDungeonMap(map.getId())
                && !FieldLimit.CANNOTMIGRATE.check(map.getFieldLimit())
                && map.allowsTeleportRock();
    }

    @Override
    public void execute(Client c, String[] params) {
        var player = c.getPlayer();
        if (player == null) return;
        if (!player.isAlive() || player.isChangingMaps() || player.getTrade() != null
                || player.getShop() != null || player.getPlayerShop() != null
                || player.getMiniGame() != null || player.getHiredMerchant() != null
                || player.getCashShop().isOpened() || c.getCM() != null
                || player.getEventInstance() != null || !regular(player.getMap())) {
            player.yellowMessage("当前状态或地图不能使用世界地图传送。");
            return;
        }
        int id;
        try {
            if (params.length != 1) throw new NumberFormatException();
            id = Integer.parseInt(params[0]);
        } catch (NumberFormatException e) {
            player.yellowMessage("请选择有效的世界地图目的地。");
            return;
        }
        if (!acceptRequest(player, System.nanoTime())) {
            player.yellowMessage("请稍候 3 秒再请求传送。");
            return;
        }
        // Validate before loading an arbitrary map; GM status does not bypass restrictions.
        if (!destinations().contains(id)) {
            player.yellowMessage("该地图不是普通世界地图目的地。");
            return;
        }
        MapleMap target = c.getChannelServer().getMapFactory().getMap(id);
        if (!regular(target)) {
            player.yellowMessage("该地图不开放直接传送。");
            return;
        }
        Portal portal = target.getPortal(0);
        if (portal == null) {
            player.yellowMessage("该地图没有可用出生点。");
            return;
        }
        if (player.getMapId() == id) return;
        player.saveLocationOnWarp();
        player.changeMap(target, portal);
    }
}
