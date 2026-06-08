package bot.service;

import bot.db.Database;
import bot.db.dao.PaymentDao;
import bot.db.model.Payment;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class PaymentService {

    public static long createPayment(long userId, double amount, String currency, String payload) {
        Payment p = new Payment();
        p.userId = userId;
        p.amount = amount;
        p.currency = currency;
        p.payload = payload;
        return PaymentDao.create(p);
    }

    public static void confirmPayment(long paymentId, long userId, double amount) {
        try (Connection c = Database.get()) {
            c.setAutoCommit(false);
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
                c.commit();
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException ignored) {}
                throw new RuntimeException("confirmPayment failed", e);
            } finally {
                try { c.setAutoCommit(true); } catch (SQLException ignored) {}
            }
        } catch (SQLException e) {
            throw new RuntimeException("confirmPayment connection failed", e);
        }
    }
}
