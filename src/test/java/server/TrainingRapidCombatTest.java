package server;

import client.Character;
import org.junit.jupiter.api.Test;
import server.maps.MapleMap;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TrainingRapidCombatTest {
    @Test void ratesRemainStableAndIdleCannotAccumulateAnUnlimitedBurst() {
        for (int rate : new int[]{10,20,40}) {
            TrainingRapidCombat.Gate gate = new TrainingRapidCombat.Gate(rate,0);
            int accepted=0;
            for (long now=0;now<10_000_000_000L;now+=1_000_000L)
                if (gate.allow(now)) accepted++;
            assertEquals(rate*10,accepted);
            long resumed=60_000_000_000L;
            assertTrue(gate.allow(resumed)); assertTrue(gate.allow(resumed));
            assertFalse(gate.allow(resumed));
        }
        assertThrows(IllegalArgumentException.class,()->new TrainingRapidCombat.Gate(100,0));
    }
    @Test void jitterAndRepeatedCallsCannotExceedTheBudget() {
        TrainingRapidCombat.Gate gate=new TrainingRapidCombat.Gate(20,0);
        assertTrue(gate.allow(0)); assertFalse(gate.allow(0));
        assertFalse(gate.allow(49_000_000)); assertTrue(gate.allow(50_000_000));
        assertFalse(gate.allow(50_000_000));
        assertTrue(gate.allow(150_000_000)); assertTrue(gate.allow(150_000_000));
        assertFalse(gate.allow(150_000_000));
    }
    private Character player() {
        Character p=mock(Character.class);
        when(p.getMap()).thenReturn(mock(MapleMap.class));
        when(p.isAlive()).thenReturn(true); when(p.isLoggedin()).thenReturn(true);
        when(p.getSkillLevel(5001001)).thenReturn(1);
        return p;
    }
    @Test void sameSettingsAreIdempotentWrongSkillsAreRejectedAndStopRestoresOrdinaryHandling() {
        Character p=player();
        TrainingRapidCombat.start(p,5001001,20);
        assertTrue(TrainingRapidCombat.allowAttack(p,5001001));
        TrainingRapidCombat.start(p,5001001,20); // cannot refill the bucket via repeats
        assertFalse(TrainingRapidCombat.allowAttack(p,5001002));
        TrainingRapidCombat.stop(p);
        assertFalse(TrainingRapidCombat.active(p));
        assertTrue(TrainingRapidCombat.allowAttack(p,5001002));
    }
    @Test void mapInstanceDeathDisconnectAndChangingMapsInvalidateTheSession() {
        for (int condition=0;condition<4;condition++) {
            Character p=player(); TrainingRapidCombat.start(p,5001001,40);
            assertTrue(TrainingRapidCombat.active(p));
            switch(condition) {
                case 0 -> when(p.getMap()).thenReturn(mock(MapleMap.class));
                case 1 -> when(p.isAlive()).thenReturn(false);
                case 2 -> when(p.isLoggedin()).thenReturn(false);
                case 3 -> when(p.isChangingMaps()).thenReturn(true);
            }
            assertFalse(TrainingRapidCombat.active(p));
        }
    }
    @Test void onlyLearnedSupportedSkillsAndThreeRatesCanStart() {
        Character p=player();
        assertThrows(IllegalArgumentException.class,()->TrainingRapidCombat.start(p,5001002,20));
        assertThrows(IllegalArgumentException.class,()->TrainingRapidCombat.start(p,0,20));
        assertThrows(IllegalArgumentException.class,()->TrainingRapidCombat.start(p,5001001,60));
        when(p.isAlive()).thenReturn(false);
        assertThrows(IllegalArgumentException.class,()->TrainingRapidCombat.start(p,5001001,20));
    }
}
