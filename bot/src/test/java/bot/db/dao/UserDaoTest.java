package bot.db.dao;

import bot.db.Database;
import bot.db.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UserDaoTest {

    @BeforeEach
    void setUp() {
        Database.initWithUrl("jdbc:sqlite:file::memory:?cache=shared");
    }

    @AfterEach
    void tearDown() {
        Database.close();
    }

    @Test
    void shouldFindUserByTgId() throws Exception {
        insertUser(12345, "john", "John Doe");
        User u = UserDao.findByTgId(12345);
        assertNotNull(u);
        assertEquals("john", u.username);
        assertEquals("John Doe", u.fullName);
    }

    @Test
    void shouldReturnNullForMissingUser() {
        assertNull(UserDao.findByTgId(99999));
    }

    @Test
    void shouldCountKeysAndPayments() throws Exception {
        insertUser(111, "u1", "U1");
        long uid = UserDao.getIdByTgId(111);
        assertNotNull(uid);

        long sid;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO servers (name, location, url, username, password) VALUES ('Test','RU','http://test','u','p')",
                     java.sql.Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (java.sql.ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                sid = rs.getLong(1);
            }
        }

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO keys (user_id, server_id, inbound_id, xui_client_id, xui_email) VALUES (?,?,1,'cid','email')")) {
            ps.setLong(1, uid);
            ps.setLong(2, sid);
            ps.executeUpdate();
        }

        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO payments (user_id, amount, status) VALUES (?,10,'completed')")) {
            ps.setLong(1, uid);
            ps.executeUpdate();
        }

        assertEquals(1, UserDao.countKeys(uid));
        assertEquals(1, UserDao.countCompletedPayments(uid));
    }

    @Test
    void shouldFindAllUsers() throws Exception {
        insertUser(1, "a", "A");
        insertUser(2, "b", "B");
        List<User> users = UserDao.findAll(10);
        assertEquals(2, users.size());
    }

    private void insertUser(long tgId, String username, String fullName) throws Exception {
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(
                     "INSERT INTO users (tg_id, username, full_name) VALUES (?,?,?)")) {
            ps.setLong(1, tgId);
            ps.setString(2, username);
            ps.setString(3, fullName);
            ps.executeUpdate();
        }
    }
}
