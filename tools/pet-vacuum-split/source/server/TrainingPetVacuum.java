package server;

import client.Character;
import server.maps.MapItem;
import server.maps.MapObjectType;
import server.maps.MapleMap;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

/** Pet-assisted full-map loot; ordinary pickup owns all rewards and item locking.
 * Reference: v83 Cosmic PetLootHandler / Character.pickupItem (AGPL-3.0).
 * Unlike GM ForceVac, never grant rewards or remove drops directly.
 */
public final class TrainingPetVacuum implements Runnable {
    private final Character player;
    private final MapleMap map;
    private volatile boolean active = true;
    private volatile ScheduledFuture<?> task;
    private int nextDrop;
    public TrainingPetVacuum(Character player) { this.player = player; map = player.getMap(); }
    public static boolean accepts(int mask, boolean mesos) { return (mask & (mesos ? 1 : 2)) != 0; }
    public static int petIndex(Character player) {
        for (int i = 0; i < 3; i++) {
            var pet = player.getPet(i);
            if (pet != null && pet.isSummoned()) return i;
        }
        return -1;
    }
    public synchronized void start() { task = TimerManager.getInstance().register(this, 500, 500); }
    public boolean isActive() { return active; }
    public void stop() { active = false; if (task != null) task.cancel(false); }
    @Override public synchronized void run() {
        if (!active) return;
        int pet = petIndex(player);
        if (!player.isLoggedin() || !player.isAlive() || player.isChangingMaps()
                || player.getMap() != map || !TrainingAssist.isAllowed(map) || pet < 0) {
            stop(); return;
        }
        // Pause during incompatible interactions. No client-supplied coordinates or rewards.
        var c = player.getClient();
        if (player.getTrade() != null || player.getShop() != null || player.getPlayerShop() != null
                || player.getMiniGame() != null || player.getHiredMerchant() != null
                || player.getCashShop().isOpened() || c.getCM() != null) return;
        try {
            int attempts = 0;
            var drops = map.getMapObjectsInRange(player.getPosition(), Double.POSITIVE_INFINITY,
                    List.of(MapObjectType.ITEM));
            // Rotate batches so a full inventory cannot starve coins behind failed item pickups.
            int start = drops.isEmpty() ? 0 : Math.floorMod(nextDrop, drops.size());
            for (int n = 0; n < drops.size(); n++) {
                int index = (start + n) % drops.size();
                var object = drops.get(index);
                nextDrop = index + 1;
                if (!active || player.getMap() != map || !player.isAlive() || player.isChangingMaps()) break;
                var item = (MapItem) object;
                if (item.isPickedUp() || item.isPlayerDrop() || !item.canBePickedBy(player)
                        || System.currentTimeMillis() - item.getDropTime() < 400) continue;
                int mask = player.getTrainingPetVacuumMask();
                if (!accepts(mask, item.getMeso() > 0)) continue;
                int id = item.getMeso() > 0 ? Integer.MAX_VALUE : item.getItemId();
                if (player.isEquippedPetItemIgnore() && player.getExcludedItems().contains(id)) continue;
                if (!player.needQuestItem(item.getQuest(), item.getItemId())) continue;
                // Bounded batches, every eligible drop remains available on the next pass.
                player.pickupItem(item, pet);
                if (++attempts >= 40) break;
            }
        } catch (RuntimeException ex) {
            stop();
            org.slf4j.LoggerFactory.getLogger(TrainingPetVacuum.class).warn("Pet vacuum stopped", ex);
        }
    }
}
