package bot.db;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseTest {

    @BeforeEach
    void setUp() {
        Database.initWithUrl("jdbc:sqlite:file::memory:?cache=shared");
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void shouldInitializeTables() throws Exception {
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery("SELECT name FROM sqlite_master WHERE type='table'")) {
            int count = 0;
            while (rs.next()) count++;
            assertTrue(count >= 4, "Expected at least 4 tables");
        }
    }

    @Test
    void shouldProvideConnection() throws Exception {
        try (Connection c = Database.get()) {
            assertNotNull(c);
            assertFalse(c.isClosed());
        }
    }
}
