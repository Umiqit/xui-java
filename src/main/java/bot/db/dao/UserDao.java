package bot.db.dao;

import bot.db.Database;
import bot.db.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public static User findByTgId(long tgId) {
        String sql = "SELECT id, tg_id, username, full_name, balance, created_at FROM users WHERE tg_id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.findByTgId failed", e);
        }
        return null;
    }

    public static Long getIdByTgId(long tgId) {
        String sql = "SELECT id FROM users WHERE tg_id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tgId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getLong("id") : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.getIdByTgId failed", e);
        }
    }

    public static List<User> findAll(int limit) {
        String sql = "SELECT id, tg_id, username, full_name, balance, created_at FROM users ORDER BY created_at DESC LIMIT ?";
        List<User> list = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.findAll failed", e);
        }
        return list;
    }

    public static int countKeys(long userId) {
        String sql = "SELECT COUNT(*) FROM keys WHERE user_id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.countKeys failed", e);
        }
    }

    public static int countCompletedPayments(long userId) {
        String sql = "SELECT COUNT(*) FROM payments WHERE user_id=? AND status='completed'";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.countCompletedPayments failed", e);
        }
    }

    public static boolean deductBalance(long userId, double amount) {
        String sql = "UPDATE users SET balance = balance - ? WHERE id=? AND balance >= ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setLong(2, userId);
            ps.setDouble(3, amount);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.deductBalance failed", e);
        }
    }

    public static void addBalance(long userId, double amount) {
        String sql = "UPDATE users SET balance = balance + ? WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setDouble(1, amount);
            ps.setLong(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("UserDao.addBalance failed", e);
        }
    }

    private static User mapRow(ResultSet rs) throws SQLException {
        return new User(
                rs.getLong("id"),
                rs.getLong("tg_id"),
                rs.getString("username"),
                rs.getString("full_name"),
                rs.getDouble("balance"),
                rs.getString("created_at")
        );
    }
}
