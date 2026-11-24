package server;

/** Monotonic, per-session activity deadline. Movement deliberately does not touch it. */
public final class JobSwitchQuietPeriod {
    private long lastActivity = System.nanoTime();

    public synchronized void touch() { touch(System.nanoTime()); }
    synchronized void touch(long now) { lastActivity = now; }
    public synchronized int remainingSeconds() { return remainingSeconds(System.nanoTime()); }
    synchronized int remainingSeconds(long now) {
        long elapsed = Math.max(0, now - lastActivity);
        return (int) Math.max(0, (30_000_000_000L - elapsed + 999_999_999L) / 1_000_000_000L);
    }
}
