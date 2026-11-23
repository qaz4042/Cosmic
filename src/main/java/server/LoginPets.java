package server;

import client.Character;
import client.inventory.InventoryType;
import client.inventory.Item;
import client.inventory.Pet;
import constants.id.ItemId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/** Prepares pets before the initial character/map packets, on a fresh login only. */
public final class LoginPets {
    private LoginPets() {
    }

    public static void prepare(Character player, long now) {
        List<Item> items = player.getInventory(InventoryType.CASH).list().stream()
                .sorted(Comparator.comparingInt(Item::getPosition)).toList();
        List<Pet> selected = items.stream()
                .filter(item -> item.getPosition() > 0 && item.getQuantity() > 0)
                .filter(item -> item.getExpiration() == -1 || item.getExpiration() > now)
                .filter(item -> item.getItemId() != ItemId.DRAGON_PET && item.getItemId() != ItemId.ROBO_PET)
                .map(Item::getPet).filter(java.util.Objects::nonNull)
                .limit(3).toList();

        // Rebuild in inventory order; normal map entry sends showPet and login registers hunger.
        for (Pet pet : Arrays.copyOf(player.getPets(), 3)) {
            if (pet != null) {
                player.removePet(pet, false);
            }
        }
        for (Item item : items) {
            Pet pet = item.getPet();
            if (pet != null) {
                boolean summoned = selected.contains(pet);
                if (pet.isSummoned() != summoned) {
                    pet.setSummoned(summoned);
                    pet.saveToDb();
                }
            }
        }
        selected.forEach(player::addPet);
    }
}
