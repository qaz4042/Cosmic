package server;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class JobSwitchQuietPeriodTest {
    @Test void thirtySecondsBoundaryUsesCeilingAndEveryActivityRestartsIt() {
        JobSwitchQuietPeriod quiet = new JobSwitchQuietPeriod();
        quiet.touch(100);
        assertEquals(30, quiet.remainingSeconds(100));
        assertEquals(30, quiet.remainingSeconds(999_999_999L));
        assertEquals(1, quiet.remainingSeconds(30_000_000_099L));
        assertEquals(0, quiet.remainingSeconds(30_000_000_100L));
        assertEquals(0, quiet.remainingSeconds(60_000_000_100L));
        quiet.touch(60_000_000_100L);
        assertEquals(30, quiet.remainingSeconds(60_000_000_100L));
        assertEquals(30, quiet.remainingSeconds(1));
    }
}
