package server;

import client.Character;
import server.maps.MapleMap;
import tools.PacketCreator;
import java.util.Map;
import java.util.WeakHashMap;

/** Explicit, per-session fast casting. Never bypasses damage, range or resource validation. */
public final class TrainingRapidCombat {
    private TrainingRapidCombat() {}
    private static final Map<Character, Session> sessions = new WeakHashMap<>();
    public static boolean supported(int skill) {
        return skill == 5001001 || skill == 5001002 || skill == 5001003;
    }
    public static boolean validRate(int rate) { return rate == 10 || rate == 20 || rate == 40; }

    // Capacity two absorbs minor packet jitter; idle time cannot accumulate a burst.
    public static final class Gate {
        private final int rate;
        private double tokens = 1;
        private long previous;
        public Gate(int rate, long now) {
            if (!validRate(rate)) throw new IllegalArgumentException("rate");
            this.rate = rate; previous = now;
        }
        public boolean allow(long now) {
            long elapsed = Math.max(0, now - previous);
            previous = now;
            tokens = Math.min(2, tokens + elapsed * (rate / 1_000_000_000.0));
            if (tokens < 1 - 1e-9) return false;
            tokens = Math.max(0, tokens - 1); return true;
        }
    }
    private static final class Session {
        final MapleMap map;
        final int skill, rate;
        final long started = System.nanoTime();
        final Gate gate;
        long accepted, rejected, lines, damage;
        Session(MapleMap map, int skill, int rate) {
            this.map = map; this.skill = skill; this.rate = rate;
            gate = new Gate(rate, started);
        }
    }
    public static synchronized void start(Character player, int skill, int rate) {
        if (!supported(skill) || !validRate(rate) || !player.isAlive()
                || !player.isLoggedin() || player.isChangingMaps() || player.getMap() == null
                || player.getSkillLevel(skill) <= 0) throw new IllegalArgumentException("rapid");
        Session old = current(player);
        if (old == null || old.skill != skill || old.rate != rate)
            sessions.put(player, new Session(player.getMap(), skill, rate));
    }
    public static synchronized boolean stop(Character player) { return sessions.remove(player) != null; }
    private static Session current(Character player) {
        Session s = sessions.get(player);
        if (s != null && (s.map != player.getMap() || !player.isAlive()
                || !player.isLoggedin() || player.isChangingMaps())) {
            sessions.remove(player); return null;
        }
        return s;
    }
    public static synchronized boolean active(Character player) { return current(player) != null; }
    /** Called before costs/damage; only the selected attack is allowed during fast mode. */
    public static synchronized boolean allowAttack(Character player, int skill) {
        Session s = current(player);
        if (s == null) return true;
        if (s.skill != skill || !s.gate.allow(System.nanoTime())) { s.rejected++; return false; }
        s.accepted++; return true;
    }
    public static synchronized void recordDamage(Character player, int skill, long damage, long positiveLines) {
        Session s = current(player);
        if (s != null && s.skill == skill && damage > 0) {
            s.damage += damage; s.lines += positiveLines;
        }
    }
    public static synchronized void sendState(Character player) {
        if (player.getMap() == null) return;
        Session s = current(player);
        String values = s == null ? "-1 0 0 0 0 0 0"
                : s.skill+" "+s.rate+" "+s.accepted+" "+s.rejected+" "+s.lines+" "+s.damage
                    +" "+Math.max(1,(System.nanoTime()-s.started)/1_000_000);
        player.sendPacket(PacketCreator.serverNotice(5,
                "[TrainingAssist:rapid:v1] "+player.getMapId()+" "+values));
    }
}
