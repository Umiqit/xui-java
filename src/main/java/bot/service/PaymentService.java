package bot.service;

import bot.db.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class PaymentService {

    public static long createPayment(long userId, double amount, String currency, String payload) {
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(
                "INSERT INTO payments (user_id, amount, currency, payload, status) VALUES (?,?,?,?,'pending')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, userId);
            ps.setDouble(2, amount);
            ps.setString(3, currency);
            ps.setString(4, payload);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("createPayment failed", e);
        }
    }

    public static void confirmPayment(long paymentId, long userId, double amount) {
        Connection c = Database.get();
        try {
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE payments SET status='completed' WHERE id=?")) {
                ps.setLong(1, paymentId);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "UPDATE users SET balance = balance + ? WHERE id=?")) {
                ps.setDouble(1, amount);
                ps.setLong(2, userId);
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            throw new RuntimeException("confirmPayment failed", e);
        }
    }
}
