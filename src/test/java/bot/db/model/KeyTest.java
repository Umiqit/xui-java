package bot.db.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class KeyTest {

    @Test
    void shouldCalculateTrafficInGb() {
        Key k = new Key();
        k.trafficUp = 512 * 1024 * 1024;   // 512 MB
        k.trafficDown = 512 * 1024 * 1024; // 512 MB
        assertEquals(1.0, k.trafficUsedGb());
    }

    @Test
    void shouldFormatExpiryDate() {
        Key k = new Key();
        k.expiryTs = 0;
        assertEquals("Бессрочно", k.expiryDate());
    }

    @Test
    void shouldDetectExpired() {
        Key k = new Key();
        k.expiryTs = System.currentTimeMillis() - 1000;
        assertTrue(k.isExpired());
    }

    @Test
    void shouldDetectNotExpired() {
        Key k = new Key();
        k.expiryTs = System.currentTimeMillis() + 86400000;
        assertFalse(k.isExpired());
    }
}
