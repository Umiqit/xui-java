package bot.db.dao;

import bot.db.Database;
import bot.db.model.Product;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ProductDao {

    public static List<Product> findAllActive() {
        String sql = "SELECT id, name, description, price, duration_days, traffic_gb, inbound_id, sort_order, active FROM products WHERE active=1 ORDER BY sort_order, id";
        List<Product> list = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ProductDao.findAllActive failed", e);
        }
        return list;
    }

    public static Product findById(long id) {
        String sql = "SELECT id, name, description, price, duration_days, traffic_gb, inbound_id, sort_order, active FROM products WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ProductDao.findById failed", e);
        }
    }

    private static Product mapRow(ResultSet rs) throws SQLException {
        return new Product(
                rs.getLong("id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getDouble("price"),
                rs.getInt("duration_days"),
                rs.getInt("traffic_gb"),
                rs.getInt("inbound_id"),
                rs.getInt("sort_order"),
                rs.getBoolean("active")
        );
    }
}
