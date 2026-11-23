package server;

import client.Character;
import client.inventory.Inventory;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.Pet;
import constants.id.ItemId;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.*;

class LoginPetsTest {
    private Item item(int slot, long expiry, int id, Pet pet) {
        Item item = mock(Item.class);
        when(item.getPosition()).thenReturn((short) slot);
        when(item.getQuantity()).thenReturn((short) 1);
        when(item.getExpiration()).thenReturn(expiry);
        when(item.getItemId()).thenReturn(id);
        when(item.getPet()).thenReturn(pet);
        return item;
    }

    @Test
    void selectsFirstThreeValidPetsInSlotOrderAndReplacesPreviousSelection() {
        Character player = mock(Character.class);
        Inventory cash = mock(Inventory.class);
        when(player.getInventory(InventoryType.CASH)).thenReturn(cash);
        Pet first = mock(Pet.class), second = mock(Pet.class), third = mock(Pet.class);
        Pet fourth = mock(Pet.class), expired = mock(Pet.class), egg = mock(Pet.class);
        when(fourth.isSummoned()).thenReturn(true);
        when(player.getPets()).thenReturn(new Pet[]{fourth, null, null});
        List<Item> items = List.of(
                item(9, -1, 5000000, fourth), item(4, -1, 5000001, second),
                item(1, 99, 5000002, expired), item(2, -1, ItemId.DRAGON_PET, egg),
                item(3, -1, 5000003, first), item(5, 101, 5000004, third));
        when(cash.list()).thenReturn(items);

        LoginPets.prepare(player, 100);

        var order = inOrder(player);
        order.verify(player).removePet(fourth, false);
        order.verify(player).addPet(first);
        order.verify(player).addPet(second);
        order.verify(player).addPet(third);
        verify(player, times(3)).addPet(any());
        verify(fourth).setSummoned(false);
        verify(fourth).saveToDb();
        verify(first).setSummoned(true);
        verify(first).saveToDb();
        verify(expired, never()).setSummoned(true);
        verify(egg, never()).setSummoned(true);
    }

    @Test
    void emptyInventoryDoesNotCreatePets() {
        Character player = mock(Character.class);
        Inventory cash = mock(Inventory.class);
        when(player.getInventory(InventoryType.CASH)).thenReturn(cash);
        when(player.getPets()).thenReturn(new Pet[3]);
        when(cash.list()).thenReturn(List.of());
        LoginPets.prepare(player, 100);
        verify(player, never()).addPet(any());
    }
}
