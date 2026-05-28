package bot.db.dao;

import bot.db.Database;
import bot.db.model.Order;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class OrderDao {

    public static long create(Order o) {
        String sql = "INSERT INTO orders (user_id, product_id, key_id, amount, status) VALUES (?,?,?,?,?)";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, o.userId);
            ps.setLong(2, o.productId);
            if (o.keyId != null) ps.setLong(3, o.keyId); else ps.setNull(3, java.sql.Types.BIGINT);
            ps.setDouble(4, o.amount);
            ps.setString(5, o.status);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("OrderDao.create failed", e);
        }
    }

    public static List<Order> findByUserId(long userId) {
        String sql = """
                SELECT o.id, o.user_id, o.product_id, o.key_id, o.amount, o.status, o.created_at, p.name as product_name
                FROM orders o
                JOIN products p ON p.id = o.product_id
                WHERE o.user_id = ?
                ORDER BY o.created_at DESC
                """;
        List<Order> list = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("OrderDao.findByUserId failed", e);
        }
        return list;
    }

    private static Order mapRow(ResultSet rs) throws SQLException {
        long keyId = rs.getLong("key_id");
        return new Order(
                rs.getLong("id"),
                rs.getLong("user_id"),
                rs.getLong("product_id"),
                rs.wasNull() ? null : keyId,
                rs.getDouble("amount"),
                rs.getString("status"),
                rs.getString("created_at"),
                rs.getString("product_name")
        );
    }
}
