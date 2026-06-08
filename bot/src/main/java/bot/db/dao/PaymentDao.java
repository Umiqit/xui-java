package bot.db.dao;

import bot.db.Database;
import bot.db.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PaymentDao {

    public static long create(Payment p) {
        String sql = "INSERT INTO payments (user_id, amount, currency, payload, status) VALUES (?,?,?,?,'pending')";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, p.userId);
            ps.setDouble(2, p.amount);
            ps.setString(3, p.currency);
            ps.setString(4, p.payload);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("PaymentDao.create failed", e);
        }
    }

    public static void updateStatus(long paymentId, String status) {
        String sql = "UPDATE payments SET status=? WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setLong(2, paymentId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("PaymentDao.updateStatus failed", e);
        }
    }
}
