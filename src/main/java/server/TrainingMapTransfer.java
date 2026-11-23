package server;

import java.util.Map;
import java.util.WeakHashMap;

/** Carries opt-in activities across an in-session map transfer, never across login. */
public final class TrainingMapTransfer {
    private TrainingMapTransfer() {}

    // The adapter keeps this logic buildable without the deployment's other training overlays.
    public interface Actor {
        Object getTrainingMapKey();
        boolean isTrainingMapAllowed();
        boolean isAlive();
        boolean isLoggedin();
        boolean isTrainingInvulnerable();
        void setTrainingInvulnerable(boolean enabled);
        boolean isTrainingSilenceEnabled();
        void setTrainingSilenceEnabled(boolean enabled);
        boolean isTrainingVacuumActive();
        boolean startTrainingVacuum();
        boolean isTrainingPetVacuumActive();
        boolean setTrainingPetVacuum(boolean enabled);
        int getTrainingAutoSkill();
        void setTrainingAutoSkill(int skill);
        void yellowMessage(String message);
    }

    private record Settings(boolean invincible, boolean silence, boolean vacuum, boolean pet, int skill) {
        static Settings capture(Actor player) {
            return new Settings(player.isTrainingInvulnerable(), player.isTrainingSilenceEnabled(),
                    player.isTrainingVacuumActive(), player.isTrainingPetVacuumActive(), player.getTrainingAutoSkill());
        }
    }
    private record Pending(Object destination, Settings settings) {}
    private static final Map<Actor, Pending> pending = new WeakHashMap<>();

    /** Must run before removePlayer cancels the old map's timers and pins. */
    public static void prepare(Actor player, Object destination) {
        if (!player.isAlive() || !player.isLoggedin()) { cancel(player); return; }
        Pending previous;
        synchronized (TrainingMapTransfer.class) { previous = pending.get(player); }
        // A chained script warp can happen before the first map-finished packet.
        Settings settings = previous != null && previous.destination == player.getTrainingMapKey()
                ? previous.settings : Settings.capture(player);
        synchronized (TrainingMapTransfer.class) { pending.put(player, new Pending(destination, settings)); }
    }

    /** Restore protection before the new map can expose the character to damage. */
    public static void enter(Actor player) {
        Pending transfer;
        synchronized (TrainingMapTransfer.class) { transfer = pending.get(player); }
        transfer = matching(player, transfer);
        if (transfer == null) return;
        if (transfer.settings.invincible) player.setTrainingInvulnerable(true);
        // Silence is shared: an unchecked preference never disables another map's rule.
        if (transfer.settings.silence) player.setTrainingSilenceEnabled(true);
    }

    /** Timers restart only after the client confirms arrival; duplicate packets do nothing. */
    public static boolean complete(Actor player) {
        Pending transfer;
        synchronized (TrainingMapTransfer.class) { transfer = pending.remove(player); }
        transfer = matching(player, transfer);
        if (transfer == null) return false;
        Settings settings = transfer.settings;
        if (settings.vacuum && !player.startTrainingVacuum())
            player.yellowMessage("切图后未能恢复吸怪：请检查落脚点或其他玩家占用。");
        if (settings.pet && !player.setTrainingPetVacuum(true))
            player.yellowMessage("切图后未能恢复宠吸：请检查宠物是否已召唤。");
        if (settings.skill >= 0) player.setTrainingAutoSkill(settings.skill);
        return true;
    }

    public static synchronized void cancel(Actor player) { pending.remove(player); }

    private static Pending matching(Actor player, Pending transfer) {
        return transfer != null && transfer.destination == player.getTrainingMapKey()
                && player.isAlive() && player.isLoggedin() && player.isTrainingMapAllowed() ? transfer : null;
    }
}
