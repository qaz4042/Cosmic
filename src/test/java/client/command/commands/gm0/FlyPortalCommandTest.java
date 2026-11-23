package client.command.commands.gm0;

import client.Client;
import org.junit.jupiter.api.Test;
import server.maps.MapleMap;
import server.maps.Portal;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlyPortalCommandTest {
    @Test void onlyOpenUpKeyPortalsCanBeEntered() {
        assertFalse(FlyPortalCommand.enterable(null));
        Portal portal = mock(Portal.class);
        when(portal.getPortalStatus()).thenReturn(true);
        for (int type : new int[]{1,2,6,7,8,10,11}) {
            when(portal.getType()).thenReturn(type);
            assertTrue(FlyPortalCommand.enterable(portal));
        }
        for (int type : new int[]{0,3,9,12,13}) {
            when(portal.getType()).thenReturn(type);
            assertFalse(FlyPortalCommand.enterable(portal));
        }
        when(portal.getType()).thenReturn(2);
        when(portal.getPortalStatus()).thenReturn(false);
        assertFalse(FlyPortalCommand.enterable(portal));
    }
    @Test void cooldownIsPerCharacterAndExpires() {
        var cmd = new FlyPortalCommand();
        var a = mock(client.Character.class);
        var b = mock(client.Character.class);
        assertTrue(cmd.acceptRequest(a,0));
        assertFalse(cmd.acceptRequest(a,699_999_999L));
        assertTrue(cmd.acceptRequest(b,1));
        assertTrue(cmd.acceptRequest(a,700_000_000L));
    }
    @Test void usesOnlyCurrentMapPortalWithoutMovingTheCharacter() {
        Client c = mock(Client.class, RETURNS_DEEP_STUBS);
        var p = c.getPlayer();
        when(p.isAlive()).thenReturn(true);
        when(p.getMapId()).thenReturn(100000000);
        when(p.getTrade()).thenReturn(null);
        when(p.getShop()).thenReturn(null);
        when(p.getPlayerShop()).thenReturn(null);
        when(p.getMiniGame()).thenReturn(null);
        when(p.getHiredMerchant()).thenReturn(null);
        when(c.getCM()).thenReturn(null);
        Portal portal = mock(Portal.class);
        when(p.getMap().getPortal("east")).thenReturn(portal);
        when(portal.getType()).thenReturn(7); // scripted door
        when(portal.getPortalStatus()).thenReturn(true);
        var cmd = new FlyPortalCommand();
        cmd.execute(c,new String[]{"999999999","east"});
        verify(portal,never()).enterPortal(c);
        cmd.execute(c,new String[]{"100000000","east"});
        verify(portal).enterPortal(c);
        verify(p,never()).setPosition(any());
        when(p.isChangingMaps()).thenReturn(true);
        assertFalse(FlyPortalCommand.usable(c));
        when(p.isChangingMaps()).thenReturn(false);
        when(p.isAlive()).thenReturn(false);
        assertFalse(FlyPortalCommand.usable(c));
    }
}
