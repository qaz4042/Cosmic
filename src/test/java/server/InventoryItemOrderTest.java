package server;

import client.inventory.Item;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class InventoryItemOrderTest {
    private Item item(int id) {
        Item item = mock(Item.class);
        when(item.getItemId()).thenReturn(id);
        return item;
    }

    @Test
    void sortsLevelDescendingThenNameAndKeepsDuplicateItemsTogether() {
        Map<Integer, Integer> levels = Map.of(1, 30, 2, 70, 3, 70, 4, 0, 5, 0);
        Map<Integer, String> names = Map.of(1, "Apple", 2, "Zebra", 3, "Apple", 4, "药水", 5, "药水");
        var items = new ArrayList<>(List.of(item(5), item(1), item(2), item(4), item(3), item(5), item(2)));
        var order = InventoryItemOrder.comparator(levels::get, names::get);
        items.sort(order);
        assertEquals(List.of(3, 2, 2, 1, 4, 5, 5), items.stream().map(Item::getItemId).toList());
        var once = new ArrayList<>(items);
        items.sort(order);
        assertEquals(once, items);
    }

    @Test
    void sortsItemsWithoutLevelsByNameAndToleratesMissingNames() {
        Map<Integer, String> names = Map.of(1, "Zebra", 2, "Apple");
        var items = new ArrayList<>(List.of(item(1), item(3), item(2)));
        items.sort(InventoryItemOrder.comparator(id -> 0, names::get));
        assertEquals(List.of(3, 2, 1), items.stream().map(Item::getItemId).toList());
    }
}
