package server;

import client.Character;
import client.Job;
import client.inventory.InventoryType;
import client.inventory.Item;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdventurerJobSwitchTest {
    @Test void refundsOnlySecondJobAndDoesNotMintPointsOnRoundTrip() {
        int first = AdventurerJobSwitch.refundedSp(110, 7,
                Map.of(1001004, 20, 1000, 3, 1100000, 20, 1101004, 10));
        assertEquals(37, first);
        assertEquals(first, AdventurerJobSwitch.refundedSp(120, first,
                Map.of(1001004, 20, 1100000, 0)));
        assertEquals(first, AdventurerJobSwitch.refundedSp(120, 17,
                Map.of(1001004, 20, 1200000, 20, 1100000, 0)));
    }

    @Test void rejectsCrossBranchSkillsInvalidJobAndSpOverflow() {
        assertThrows(IllegalArgumentException.class,
                () -> AdventurerJobSwitch.refundedSp(110, 0, Map.of(1200000, 20)));
        assertThrows(IllegalArgumentException.class,
                () -> AdventurerJobSwitch.refundedSp(110, 0, Map.of(1111002, 1)));
        assertEquals(0, AdventurerJobSwitch.refundedSp(112, 0, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> AdventurerJobSwitch.refundedSp(110, -1, Map.of()));
        assertThrows(IllegalArgumentException.class,
                () -> AdventurerJobSwitch.refundedSp(110, 32767, Map.of(1100000, 1)));
    }

    private static final int[] JOBS = {110,120,130,210,220,230,310,320,410,420,510,520};

    @Test void allowsExactlyTheTwelveSecondJobsWithinTheirOwnFamily() {
        Character p = eligible();
        for (int source : JOBS) {
            when(p.getJob()).thenReturn(Job.getById(source));
            for (int target : JOBS) {
                boolean allowed = source != target && source / 100 == target / 100;
                assertEquals(allowed, AdventurerJobSwitch.rejection(p, target) == null,
                        source + " -> " + target);
                assertEquals(allowed && source / 100 == 5,
                        AdventurerJobSwitch.canSwapStats(source, target));
            }
        }
        for (int invalid : new int[]{0,100,200,500,113,213,323,423,523,900,1110,2100,2200}) {
            assertFalse(AdventurerJobSwitch.branch(invalid));
            assertFalse(AdventurerJobSwitch.sameFamily(invalid, 110));
        }
    }

    @Test void preservesEachFirstJobAndConservesSpAcrossEveryFamily() {
        for (int source : JOBS) {
            int firstJob = source / 100 * 100;
            int refunded = AdventurerJobSwitch.refundedSp(source, 7,
                    Map.of(firstJob * 10000 + 1000, 20, 1000, 3, source * 10000, 20));
            assertEquals(27, refunded);
            for (int target : JOBS) if (source != target && source / 100 == target / 100) {
                assertEquals(27, AdventurerJobSwitch.refundedSp(target, 17,
                        Map.of(firstJob * 10000 + 1000, 20, source * 10000, 0,
                                target * 10000, 10)));
            }
        }
        assertThrows(IllegalArgumentException.class,
                () -> AdventurerJobSwitch.refundedSp(220, 0, Map.of(2101001, 1)));
        assertThrows(IllegalArgumentException.class,
                () -> AdventurerJobSwitch.refundedSp(520, 0, Map.of(4001000, 1)));
    }

    @Test void forgedStatSwapAndStaleConfirmationsCannotReachMutation() {
        Character p = eligible();
        doCallRealMethod().when(p).setJob(any(Job.class));
        doCallRealMethod().when(p).switchAdventurerBranch(anyInt(), anyInt(), anyBoolean(), anyInt(), anyInt());
        p.setJob(Job.FIGHTER);
        assertTrue(p.switchAdventurerBranch(120, 120, false, 0, 0).contains("职业已变化"));
        assertTrue(p.switchAdventurerBranch(120, 110, true, 300, 40).contains("仅拳手"));
        p.setJob(Job.BRAWLER);
        when(p.getJob()).thenReturn(Job.BRAWLER);
        when(p.getStr()).thenReturn(300);
        when(p.getDex()).thenReturn(40);
        assertTrue(p.switchAdventurerBranch(520, 510, true, 299, 40).contains("属性已变化"));
        assertTrue(p.switchAdventurerBranch(520, 510, true, 300, 41).contains("属性已变化"));
        verify(p, never()).cancelAllBuffs(anyBoolean());
        verify(p, never()).changeJob(any(Job.class));
    }

    @Test void combatAndRestrictedMapsBlockSwitchAtFinalConfirmation() {
        Character p = eligible();
        when(p.getMap().isTown()).thenReturn(false);
        assertEquals(0, AdventurerJobSwitch.status(p));
        when(p.getJobSwitchQuietSeconds()).thenReturn(18);
        assertEquals(1, AdventurerJobSwitch.status(p));
        assertTrue(AdventurerJobSwitch.rejection(p, 120).contains("18 秒"));
        doCallRealMethod().when(p).setJob(any(Job.class));
        doCallRealMethod().when(p).switchAdventurerBranch(anyInt(), anyInt(), anyBoolean(), anyInt(), anyInt());
        p.setJob(Job.FIGHTER);
        assertTrue(p.switchAdventurerBranch(120, 110, false, 0, 0).contains("18 秒"));
        verify(p, never()).cancelAllBuffs(anyBoolean());
        when(p.getJobSwitchQuietSeconds()).thenReturn(0);
        when(p.getMap().allowsTeleportRock()).thenReturn(false);
        assertEquals(3, AdventurerJobSwitch.status(p));
        when(p.getMap().allowsTeleportRock()).thenReturn(true);
        when(p.getMapId()).thenReturn(910000000);
        assertEquals(3, AdventurerJobSwitch.status(p));
        when(p.getMapId()).thenReturn(100000000);
        assertEquals(0, AdventurerJobSwitch.status(p));
        when(p.getInventory(InventoryType.EQUIPPED).list()).thenReturn(List.of(mock(Item.class)));
        assertEquals(4, AdventurerJobSwitch.status(p));
    }

    @Test void existingSkillCooldownDoesNotBlockQuietCharacter() {
        Character p = eligible();
        when(p.getAllCooldowns()).thenReturn(List.of(mock(net.server.PlayerCoolDownValueHolder.class)));
        assertEquals(0, AdventurerJobSwitch.status(p));
        assertNull(AdventurerJobSwitch.rejection(p, 120));
        when(p.getJobSwitchQuietSeconds()).thenReturn(1);
        assertEquals(1, AdventurerJobSwitch.status(p));
        assertNotNull(AdventurerJobSwitch.rejection(p, 120));
    }

    private Character eligible() {
        Character p = mock(Character.class, RETURNS_DEEP_STUBS);
        when(p.getJob()).thenReturn(Job.FIGHTER);
        when(p.getLevel()).thenReturn(30);
        when(p.isLoggedinWorld()).thenReturn(true);
        when(p.isAlive()).thenReturn(true);
        when(p.getAllCooldowns()).thenReturn(List.of());
        when(p.getMap().allowsTeleportRock()).thenReturn(true);
        when(p.getMapId()).thenReturn(100000000);
        when(p.getMap().getEventInstance()).thenReturn(null);
        when(p.getEventInstance()).thenReturn(null);
        when(p.getTrade()).thenReturn(null);
        when(p.getShop()).thenReturn(null);
        when(p.getPlayerShop()).thenReturn(null);
        when(p.getMiniGame()).thenReturn(null);
        when(p.getHiredMerchant()).thenReturn(null);
        when(p.getInventory(InventoryType.EQUIPPED).list()).thenReturn(List.of());
        return p;
    }

    @Test void validatesBoundsStateAndEquipmentAgainAtConfirmation() {
        Character p = eligible();
        assertNull(AdventurerJobSwitch.rejection(p, 120));
        assertNotNull(AdventurerJobSwitch.rejection(p, 110));
        assertNotNull(AdventurerJobSwitch.rejection(p, 112));
        when(p.getLevel()).thenReturn(69);
        assertNull(AdventurerJobSwitch.rejection(p, 130));
        when(p.getLevel()).thenReturn(70);
        assertNull(AdventurerJobSwitch.rejection(p, 130));
        when(p.getLevel()).thenReturn(29);
        assertNotNull(AdventurerJobSwitch.rejection(p, 130));
        when(p.getLevel()).thenReturn(40);
        when(p.getMap().isTown()).thenReturn(false);
        assertNull(AdventurerJobSwitch.rejection(p, 130));
        when(p.getMap().isTown()).thenReturn(true);
        when(p.isChangingMaps()).thenReturn(true);
        assertNotNull(AdventurerJobSwitch.rejection(p, 130));
        when(p.isChangingMaps()).thenReturn(false);
        when(p.getInventory(InventoryType.EQUIPPED).list()).thenReturn(List.of(mock(Item.class)));
        assertNotNull(AdventurerJobSwitch.rejection(p, 130));
    }
}
