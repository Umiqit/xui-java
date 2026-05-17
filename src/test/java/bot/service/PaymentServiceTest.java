package bot.service;

import bot.db.Database;
import bot.db.dao.UserDao;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;

import static org.junit.jupiter.api.Assertions.*;

class PaymentServiceTest {

    @BeforeEach
    void setUp() {
        Database.initWithUrl("jdbc:sqlite:file::memory:?cache=shared");
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void shouldCreatePayment() throws Exception {
        long userId = insertUser(111, "u", "U");
        long pid = PaymentService.createPayment(userId, 100, "XTR", "test");
        assertTrue(pid > 0);
    }

    @Test
    void shouldConfirmPaymentAndUpdateBalance() throws Exception {
        long userId = insertUser(222, "u2", "U2");
        long pid = PaymentService.createPayment(userId, 50, "XTR", "test2");

        PaymentService.confirmPayment(pid, userId, 50);

        var user = UserDao.findByTgId(222);
        assertNotNull(user);
        assertEquals(50.0, user.balance, 0.001);
    }

    private long insertUser(long tgId, String username, String fullName) throws Exception {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users (tg_id, username, full_name) VALUES (?,?,?)")) {
            ps.setLong(1, tgId);
            ps.setString(2, username);
            ps.setString(3, fullName);
            ps.executeUpdate();
        }
        Long id = UserDao.getIdByTgId(tgId);
        assertNotNull(id);
        return id;
    }
}
