package server;

import client.Character;
import client.inventory.Equip;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.ModifyInventory;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Holds both inventories until the character save commits; no client updates before commit. */
public final class JobSwitchEquipment implements AutoCloseable {
    private record Move(Equip item, short source, short destination) {}
    private final Inventory worn, bag;
    private final List<Move> moves = new ArrayList<>();
    private boolean moved, committed;

    private JobSwitchEquipment(Character player) {
        worn = player.getInventory(InventoryType.EQUIPPED);
        bag = player.getInventory(InventoryType.EQUIP);
        worn.lockInventory();
        bag.lockInventory();
        try {
            var items = new ArrayList<>(worn.list());
            items.sort(Comparator.comparingInt(item -> item.getPosition()));
            List<Short> free = new ArrayList<>();
            for (short slot = 1; slot <= Byte.toUnsignedInt(bag.getSlotLimit()); slot++)
                if (bag.getItem(slot) == null) free.add(slot);
            if (free.size() < items.size())
                throw new IllegalArgumentException("装备栏空间不足：自动卸下全部装备需要 " + items.size()
                        + " 个空位，当前只有 " + free.size() + " 个。请先整理背包。");
            for (int i = 0; i < items.size(); i++) {
                if (!(items.get(i) instanceof Equip equip)) throw new IllegalArgumentException("穿戴数据异常，请联系管理员。");
                moves.add(new Move(equip, equip.getPosition(), free.get(i)));
            }
        } catch (RuntimeException e) {
            bag.unlockInventory(); worn.unlockInventory(); throw e;
        }
    }

    public static JobSwitchEquipment prepare(Character player) { return new JobSwitchEquipment(player); }
    public static boolean hasRoom(Character player) {
        try (var plan = prepare(player)) { return true; }
        catch (IllegalArgumentException e) { return false; }
    }
    public void move() {
        moved = true;
        for (Move move : moves) {
            worn.removeSlot(move.source());
            move.item().setPosition(move.destination());
            bag.addItemFromDB(move.item());
        }
    }
    public void commit(Character player) {
        committed = true; // The DB has committed; network errors must not roll back persisted equipment.
        List<ModifyInventory> updates = new ArrayList<>();
        for (Move move : moves) {
            Equip equip = move.item();
            if (equip.getRingId() > -1) {
                var ring = player.getRingById(equip.getRingId());
                if (ring != null) ring.unequip();
            }
            player.unequippedItem(equip);
            updates.add(new ModifyInventory(2, equip, move.source()));
        }
        if (!updates.isEmpty()) player.sendPacket(tools.PacketCreator.modifyInventory(true, updates));
    }
    @Override public void close() {
        try {
            if (moved && !committed) for (Move move : moves) {
                bag.removeSlot(move.destination());
                move.item().setPosition(move.source());
                worn.addItemFromDB(move.item());
            }
        } finally { bag.unlockInventory(); worn.unlockInventory(); }
    }
}
