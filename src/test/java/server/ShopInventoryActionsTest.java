package server;

import client.Character;
import client.Client;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import constants.inventory.ItemConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.mockito.MockedStatic;
import provider.DataProvider;
import provider.DataProviderFactory;
import tools.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ShopInventoryActionsTest {
    private MockedStatic<ItemInformationProvider> provider;
    private ItemInformationProvider ii;

    @BeforeAll
    static void initializeProviderWithoutDatabase() throws Exception {
        try (var database = mockStatic(DatabaseConnection.class);
             var data = mockStatic(DataProviderFactory.class)) {
            Connection connection = mock(Connection.class);
            PreparedStatement statement = mock(PreparedStatement.class);
            when(connection.prepareStatement(anyString())).thenReturn(statement);
            when(statement.executeQuery()).thenReturn(mock(ResultSet.class));
            database.when(DatabaseConnection::getConnection).thenReturn(connection);
            data.when(() -> DataProviderFactory.getDataProvider(any())).thenReturn(mock(DataProvider.class));
            Class.forName("server.ItemInformationProvider");
        }
    }

    @BeforeEach
    void mockProvider() {
        provider = mockStatic(ItemInformationProvider.class);
        ii = mock(ItemInformationProvider.class);
        provider.when(ItemInformationProvider::getInstance).thenReturn(ii);
    }

    @AfterEach
    void closeProvider() { provider.close(); }

    private final Client client = mock(Client.class);
    private final Character chr = mock(Character.class);
    private final Inventory use = new Inventory(chr, InventoryType.USE, (byte) 96);

    ShopInventoryActionsTest() {
        when(client.getPlayer()).thenReturn(chr);
        when(chr.getInventory(InventoryType.USE)).thenReturn(use);
    }

    private Item item(int id, int slot, int count) {
        Item item = new Item(id, (short) slot, (short) count);
        use.addItemFromDB(item);
        return item;
    }

    @Test
    void lockIsIdempotentAndUnlockPreservesOtherFlags() {
        Item item = item(2000000, 1, 10);
        item.setFlag(ItemConstants.UNTRADEABLE);
        ShopInventoryActions.setSellLock(client, (byte) 2, (short) 1, 2000000, true);
        ShopInventoryActions.setSellLock(client, (byte) 2, (short) 1, 2000000, true);
        assertEquals(ItemConstants.UNTRADEABLE | ItemConstants.SELL_LOCK, item.getFlag());
        ShopInventoryActions.setSellLock(client, (byte) 2, (short) 1, 2000000, false);
        assertEquals(ItemConstants.UNTRADEABLE, item.getFlag());
    }

    @Test
    void lockRejectsStaleIdentityAndInvalidInventoryOrSlot() {
        Item item = item(2000000, 1, 10);
        ShopInventoryActions.setSellLock(client, (byte) 2, (short) 1, 2000001, true);
        ShopInventoryActions.setSellLock(client, (byte) 2, (short) -1, 2000000, true);
        ShopInventoryActions.setSellLock(client, (byte) 5, (short) 1, 2000000, true);
        assertEquals(0, item.getFlag());
    }

    @Test
    void lockedStacksNeverMergeIntoUnlockedStacksAndLockFollowsMoves() {
        Item locked = item(2000000, 1, 10);
        locked.setFlag(ItemConstants.SELL_LOCK);
        Item unlocked = item(2000000, 2, 20);
        use.move((short) 1, (short) 2, (short) 100);
        assertSame(locked, use.getItem((short) 2));
        assertSame(unlocked, use.getItem((short) 1));
        assertEquals(10, locked.getQuantity());
        use.move((short) 2, (short) 9, (short) 100);
        assertEquals(ItemConstants.SELL_LOCK, use.getItem((short) 9).getFlag());
        assertEquals(ItemConstants.SELL_LOCK, locked.copy().getFlag());
    }

    @Test
    void bulkSaleRevalidatesLocksStacksPriceCashAndMesoLimit() {
        Shop shop = mock(Shop.class);
        when(chr.getShop()).thenReturn(shop);
        when(shop.getNpcId()).thenReturn(9090000);
        Item locked = item(2000000, 1, 10); locked.setFlag(ItemConstants.SELL_LOCK);
        item(2000001, 2, 20); // quantity changed since confirmation
        item(2000002, 3, 5);  // zero price
        item(2000003, 4, 5);  // cash item
        item(2000004, 5, 5);  // eligible
        item(2000005, 6, 5);  // exceeds meso cap
        item(2000006, 7, 5);  // notSale
        Item nativeLock = item(2000007, 8, 5); nativeLock.setFlag(ItemConstants.LOCK);
            when(ii.isCash(2000003)).thenReturn(true);
            when(ii.isNpcSaleBlocked(2000006)).thenReturn(true);
            when(ii.getPrice(2000004, 5)).thenReturn(50);
            when(ii.getPrice(2000005, 5)).thenReturn(100);
            when(chr.getMeso()).thenReturn(Integer.MAX_VALUE - 50);
            ShopInventoryActions.sellAll(client, 9090000, List.of(
                    new ShopInventoryActions.Sale((byte) 2, (short) 1, 2000000, (short) 10),
                    new ShopInventoryActions.Sale((byte) 2, (short) 2, 2000001, (short) 10),
                    new ShopInventoryActions.Sale((byte) 2, (short) 3, 2000002, (short) 5),
                    new ShopInventoryActions.Sale((byte) 2, (short) 4, 2000003, (short) 5),
                    new ShopInventoryActions.Sale((byte) 2, (short) 5, 2000004, (short) 5),
                    new ShopInventoryActions.Sale((byte) 2, (short) 6, 2000005, (short) 5),
                    new ShopInventoryActions.Sale((byte) 2, (short) 7, 2000006, (short) 5),
                    new ShopInventoryActions.Sale((byte) 2, (short) 8, 2000007, (short) 5),
                    new ShopInventoryActions.Sale((byte) 5, (short) 1, 5450000, (short) 1)));
            verify(shop).sell(client, InventoryType.USE, (short) 5, (short) 5);
            verify(shop).sell(client, InventoryType.USE, (short) 3, (short) 5);
            verify(shop, times(2)).sell(any(), any(), anyShort(), anyShort());
    }

    @Test
    void bulkSaleRequiresTheSameOpenShop() {
        ShopInventoryActions.sellAll(client, 9090000, List.of());
        Shop shop = mock(Shop.class);
        when(chr.getShop()).thenReturn(shop);
        when(shop.getNpcId()).thenReturn(1000);
        ShopInventoryActions.sellAll(client, 9090000, List.of());
        verify(shop, never()).sell(any(), any(), anyShort(), anyShort());
    }

    @Test
    void realBulkSaleRemovesEntireStackOnceAndPaysServerPrice() throws Exception {
        var constructor = Shop.class.getDeclaredConstructor(int.class, int.class);
        constructor.setAccessible(true);
        Shop shop = constructor.newInstance(1338, 9090000);
        when(chr.getShop()).thenReturn(shop);
        Item protectedItem = item(2000001, 2, 20);
        protectedItem.setFlag(ItemConstants.SELL_LOCK);
        item(2000000, 1, 10);
        when(ii.getPrice(2000000, 10)).thenReturn(250);
        var sale = new ShopInventoryActions.Sale((byte) 2, (short) 1, 2000000, (short) 10);
        ShopInventoryActions.sellAll(client, 9090000, List.of(sale, sale,
                new ShopInventoryActions.Sale((byte) 2, (short) 2, 2000001, (short) 20)));
        assertNull(use.getItem((short) 1));
        assertSame(protectedItem, use.getItem((short) 2));
        verify(chr, times(1)).gainMeso(250, false);
    }

    @Test
    void zeroPriceArrowStacksAreRemovedButLockedArrowsStayAndNoMesosArePaid() throws Exception {
        var constructor = Shop.class.getDeclaredConstructor(int.class, int.class);
        constructor.setAccessible(true);
        Shop shop = constructor.newInstance(1338, 9090000);
        when(chr.getShop()).thenReturn(shop);
        item(2061000, 1, 2000);
        Item locked = item(2061000, 2, 2000);
        locked.setFlag(ItemConstants.SELL_LOCK);
        item(2060000, 3, 992);
        when(chr.getMeso()).thenReturn(Integer.MAX_VALUE);
        ShopInventoryActions.sellAll(client, 9090000, List.of(
                new ShopInventoryActions.Sale((byte) 2, (short) 1, 2061000, (short) 2000),
                new ShopInventoryActions.Sale((byte) 2, (short) 2, 2061000, (short) 2000),
                new ShopInventoryActions.Sale((byte) 2, (short) 3, 2060000, (short) 992)));
        assertNull(use.getItem((short) 1));
        assertNull(use.getItem((short) 3));
        assertSame(locked, use.getItem((short) 2));
        verify(chr, never()).gainMeso(anyInt(), anyBoolean());
    }

    @Test
    void ordinarySaleAlsoRejectsProtectedItems() throws Exception {
        var constructor = Shop.class.getDeclaredConstructor(int.class, int.class);
        constructor.setAccessible(true);
        Shop shop = constructor.newInstance(1338, 9090000);
        Item locked = item(2000000, 1, 10);
        for (short flag : new short[]{ItemConstants.SELL_LOCK, ItemConstants.LOCK}) {
            locked.setFlag(flag);
            shop.sell(client, InventoryType.USE, (short) 1, (short) 10);
            assertSame(locked, use.getItem((short) 1));
            assertEquals(10, locked.getQuantity());
        }
        verify(chr, never()).gainMeso(anyInt(), anyBoolean());
    }
    @Test
    void rechargeablePricesArePerCustomerAndCachedStockStaysUnchanged() throws Exception {
        var ctor = Shop.class.getDeclaredConstructor(int.class, int.class);
        ctor.setAccessible(true);
        Shop cached = ctor.newInstance(1, 2020001);
        var field = Shop.class.getDeclaredField("items");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<ShopItem> stock = (List<ShopItem>) field.get(cached);
        stock.add(new ShopItem((short) 1000, 2330001, 0, 0));
        stock.add(new ShopItem((short) 1, 2330000, 600, 0));
        stock.add(new ShopItem((short) 1, 2070001, 0, 3));
        when(ii.getUnitPrice(2330001)).thenReturn(0.3);
        when(ii.getSlotMax(client, 2330001)).thenReturn((short) 1001, (short) 1200);
        cached.sendShop(client);
        cached.sendShop(client);
        var sessions = org.mockito.ArgumentCaptor.forClass(Shop.class);
        verify(chr, times(2)).setShop(sessions.capture());
        @SuppressWarnings("unchecked")
        List<ShopItem> first = (List<ShopItem>) field.get(sessions.getAllValues().get(0));
        @SuppressWarnings("unchecked")
        List<ShopItem> second = (List<ShopItem>) field.get(sessions.getAllValues().get(1));
        assertEquals(301, first.get(0).getPrice());
        assertEquals(360, second.get(0).getPrice());
        assertEquals(600, first.get(1).getPrice());
        assertEquals(0, first.get(2).getPrice());
        assertEquals(3, first.get(2).getPitch());
        assertEquals(0, stock.get(0).getPrice());
    }
}
