/*
    This file is part of the HeavenMS MapleStory Server, commands OdinMS-based
    Copyleft (L) 2016 - 2019 RonanLana

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU Affero General Public License as
    published by the Free Software Foundation version 3 as published by
    the Free Software Foundation. You may not use, modify or distribute
    this program under any other version of the GNU Affero General Public
    License.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU Affero General Public License for more details.

    You should have received a copy of the GNU Affero General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

/*
   @Author: Arthur L - Refactored command content into modules
*/
package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import constants.id.MapId;
import server.maps.FieldLimit;
import server.maps.MapleMap;
import server.maps.MiniDungeonInfo;
import server.maps.Portal;

public class WarpCommand extends Command {
    {
        setDescription("Warp directly to a public map by ID.");
    }

    @Override
    public void execute(Client c, String[] params) {
        Character player = c.getPlayer();
        if (params.length < 1) {
            player.yellowMessage("Syntax: @warp <mapid>");
            return;
        }

        if (!player.isAlive()) {
            player.dropMessage(1, "This command cannot be used when you're dead.");
            return;
        }

        if (!player.isGM() && (player.getEventInstance() != null
                || MiniDungeonInfo.isDungeonMap(player.getMapId())
                || FieldLimit.CANNOTMIGRATE.check(player.getMap().getFieldLimit()))) {
            player.dropMessage(1, "This command cannot be used in this map.");
            return;
        }

        MapleMap target;
        try {
            target = c.getChannelServer().getMapFactory().getMap(Integer.parseInt(params[0]));
        } catch (NumberFormatException ex) {
            target = null;
        }

        if (target == null) {
            player.yellowMessage("Map ID " + params[0] + " is invalid.");
            return;
        }

        int mapId = target.getId();
        if (!player.isGM() && (mapId == MapId.GM_MAP || mapId == MapId.JAIL
                || !target.allowsTeleportRock())) {
            player.yellowMessage("This map is unavailable for direct travel.");
            return;
        }

        Portal spawn = target.getRandomPlayerSpawnpoint();
        if (spawn == null) {
            player.yellowMessage("This map has no available spawn point.");
            return;
        }

        player.saveLocationOnWarp();
        player.changeMap(target, spawn);
    }
}
