package client.command.commands.gm0;

import org.junit.jupiter.api.Test;
import server.maps.MapleMap;
import constants.id.MapId;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class MapTravelCommandTest {
    @Test void rejectsSpecialAndRestrictedMapsEvenForGmTravel() {
        assertFalse(MapTravelCommand.regular(null));
        MapleMap map = mock(MapleMap.class);
        when(map.getId()).thenReturn(100000000);
        when(map.allowsTeleportRock()).thenReturn(true);
        assertTrue(MapTravelCommand.regular(map));
        when(map.allowsTeleportRock()).thenReturn(false);
        assertFalse(MapTravelCommand.regular(map));
        when(map.allowsTeleportRock()).thenReturn(true);
        for (int id : new int[]{-1, MapId.GM_MAP, MapId.JAIL, 910000000}) {
            when(map.getId()).thenReturn(id);
            assertFalse(MapTravelCommand.regular(map));
        }
    }

    @Test void publicDestinationAllowlistComesFromServerWz() throws Exception {
        var method = MapTravelCommand.class.getDeclaredMethod("destinations");
        method.setAccessible(true);
        var ids = (java.util.Set<?>) method.invoke(new MapTravelCommand());
        assertTrue(ids.contains(100000000));
        assertFalse(ids.contains(MapId.GM_MAP));
        assertFalse(ids.contains(-1));
    }
}
