package server;

import client.Client;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.ModifyInventory;
import constants.inventory.ItemConstants;
import tools.PacketCreator;

import java.util.List;

/** OpenStory's NPC shop extensions. Item flags use the existing persistence/wire format. */
public final class ShopInventoryActions {
    private ShopInventoryActions() {}

    public record Sale(byte type, short slot, int itemId, short quantity) {}

    private static boolean saleInventory(byte type) {
        return type >= 1 && type <= 4;
    }

    public static void setSellLock(Client c, byte type, short slot, int itemId, boolean locked) {
        var chr = c.getPlayer();
        if (!saleInventory(type) || slot <= 0 || chr.getTrade() != null
                || chr.getPlayerShop() != null || chr.getHiredMerchant() != null || chr.getMiniGame() != null) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        Inventory inv = chr.getInventory(InventoryType.getByType(type));
        inv.lockInventory();
        try {
            Item item = inv.getItem(slot);
            if (item != null && item.getItemId() == itemId) {
                short flags = item.getFlag();
                item.setFlag((short) (locked ? flags | ItemConstants.SELL_LOCK : flags & ~ItemConstants.SELL_LOCK));
                c.sendPacket(PacketCreator.modifyInventory(true,
                        List.of(new ModifyInventory(3, item), new ModifyInventory(0, item))));
            } else {
                c.sendPacket(PacketCreator.enableActions());
            }
        } finally {
            inv.unlockInventory();
        }
    }

    public static void sellAll(Client c, int npcId, List<Sale> sales) {
        Shop shop = c.getPlayer().getShop();
        if (shop == null || shop.getNpcId() != npcId) {
            c.sendPacket(PacketCreator.enableActions());
            return;
        }
        ItemInformationProvider ii = ItemInformationProvider.getInstance();
        int sold = 0;
        for (Sale sale : sales) {
            if (!saleInventory(sale.type()) || sale.slot() <= 0 || sale.quantity() <= 0) continue;
            InventoryType type = InventoryType.getByType(sale.type());
            Inventory inv = c.getPlayer().getInventory(type);
            inv.lockInventory();
            try {
                Item item = inv.getItem(sale.slot());
                // Never sell new/different stacks, protected items, cash items or items without a sale price.
                if (item == null || item.getItemId() != sale.itemId() || item.getQuantity() != sale.quantity()
                        || (item.getFlag() & (ItemConstants.SELL_LOCK | ItemConstants.LOCK)) != 0
                        || ii.isCash(item.getItemId()) || ii.isNpcSaleBlocked(item.getItemId())) continue;
                int price = ii.getPrice(item.getItemId(), item.getQuantity());
                if (price < 0 || (long) c.getPlayer().getMeso() + price > Integer.MAX_VALUE) continue;
                shop.sell(c, type, sale.slot(), sale.quantity());
                sold++;
            } finally {
                inv.unlockInventory();
            }
        }
        c.getPlayer().dropMessage(5, "批量售卖完成：售出 " + sold + " 格，已跳过加锁、不可售卖或发生变化的物品。");
        c.sendPacket(PacketCreator.enableActions());
    }
}
