package server;

import client.inventory.Item;

import java.text.Collator;
import java.util.Comparator;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.IntUnaryOperator;

/** Ordering for the inventory tidy action; same item IDs always stay together. */
public final class InventoryItemOrder {
    private InventoryItemOrder() {}

    public static Comparator<Item> comparator(IntUnaryOperator requiredLevel, IntFunction<String> name) {
        // A fresh collator per request avoids sharing its mutable state between channels.
        Collator names = Collator.getInstance(Locale.CHINA);
        names.setStrength(Collator.SECONDARY);
        return Comparator.<Item>comparingInt(item -> requiredLevel.applyAsInt(item.getItemId()))
                .reversed()
                .thenComparing(item -> {
                    String value = name.apply(item.getItemId());
                    return value == null ? "" : value;
                }, names)
                .thenComparingInt(Item::getItemId);
    }
}
