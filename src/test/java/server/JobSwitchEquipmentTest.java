package server;

import client.Character;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JobSwitchEquipmentTest {
    private Equip equip(int id, short initialPosition) {
        Equip item = mock(Equip.class);
        java.util.concurrent.atomic.AtomicInteger position = new java.util.concurrent.atomic.AtomicInteger(initialPosition);
        when(item.getItemId()).thenReturn(id);
        when(item.getPosition()).thenAnswer(call -> (short) position.get());
        doAnswer(call -> { position.set(call.getArgument(0, Short.class)); return null; })
                .when(item).setPosition(anyShort());
        when(item.getRingId()).thenReturn(-1);
        when(item.getInventoryType()).thenReturn(InventoryType.EQUIP);
        when(item.copy()).thenAnswer(call -> {
            Equip copy = mock(Equip.class);
            when(copy.getPosition()).thenReturn((short) position.get());
            when(copy.getInventoryType()).thenReturn(InventoryType.EQUIP);
            return copy;
        });
        return item;
    }
    private Character player(Inventory worn, Inventory bag) {
        Character p = mock(Character.class);
        when(p.getInventory(InventoryType.EQUIPPED)).thenReturn(worn);
        when(p.getInventory(InventoryType.EQUIP)).thenReturn(bag);
        return p;
    }
    @Test void fullBagRejectsBeforeMovingAnything() {
        Inventory worn = new Inventory(null, InventoryType.EQUIPPED, (byte) 24);
        Inventory bag = new Inventory(null, InventoryType.EQUIP, (byte) 1);
        Equip weapon = equip(1302000, (short)-11), existing = equip(1002000, (short)1);
        worn.addItemFromDB(weapon); bag.addItemFromDB(existing);
        Character p = player(worn, bag);
        assertFalse(JobSwitchEquipment.hasRoom(p));
        assertThrows(IllegalArgumentException.class, () -> JobSwitchEquipment.prepare(p));
        assertSame(weapon, worn.getItem((short)-11));
        assertSame(existing, bag.getItem((short)1));
    }
    @Test void abortRestoresEveryPositionAndEmitsNoPackets() {
        Inventory worn = new Inventory(null, InventoryType.EQUIPPED, (byte) 24);
        Inventory bag = new Inventory(null, InventoryType.EQUIP, (byte) 3);
        Equip weapon = equip(1302000, (short)-11), cash = equip(1002000, (short)-101);
        Equip existing = equip(1002001, (short)2);
        worn.addItemFromDB(weapon); worn.addItemFromDB(cash); bag.addItemFromDB(existing);
        Character p = player(worn, bag);
        try (var plan = JobSwitchEquipment.prepare(p)) {
            assertEquals(2, worn.list().size()); // preview/prepare is non-mutating
            plan.move();
            assertTrue(worn.list().isEmpty());
            assertSame(cash, bag.getItem((short)1));
            assertSame(weapon, bag.getItem((short)3));
        }
        assertSame(weapon, worn.getItem((short)-11));
        assertSame(cash, worn.getItem((short)-101));
        assertEquals(-11, weapon.getPosition()); assertEquals(-101, cash.getPosition());
        assertSame(existing, bag.getItem((short)2));
        assertNull(bag.getItem((short)1)); assertNull(bag.getItem((short)3));
        verify(p, never()).sendPacket(any());
        verify(p, never()).unequippedItem(any());
    }
    @Test void successfulCommitPreservesItemsAndNotifiesOnlyAfterSave() {
        Inventory worn = new Inventory(null, InventoryType.EQUIPPED, (byte)24);
        Inventory bag = new Inventory(null, InventoryType.EQUIP, (byte)2);
        Equip weapon = equip(1302000, (short)-11);
        worn.addItemFromDB(weapon);
        Character p = player(worn, bag);
        try (var plan = JobSwitchEquipment.prepare(p)) {
            plan.move(); verify(p, never()).sendPacket(any());
            plan.commit(p);
        }
        assertTrue(worn.list().isEmpty()); assertSame(weapon, bag.getItem((short)1));
        verify(p).unequippedItem(weapon); verify(p).sendPacket(any());
    }
}
