package bot.db.dao;

import bot.db.Database;
import bot.db.model.Key;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class KeyDao {

    public static List<Key> findByUserTgId(long tgId) {
        String sql = """
                SELECT k.*, s.name as server_name, s.location as server_location FROM keys k
                JOIN users u ON u.id = k.user_id
                LEFT JOIN servers s ON s.id = k.server_id
                WHERE u.tg_id = ?
                ORDER BY k.created_at DESC
                """;
        List<Key> list = new ArrayList<>();
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tgId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new RuntimeException("KeyDao.findByUserTgId failed", e);
        }
        return list;
    }

    public static Key findByIdAndUserTgId(long keyId, long tgId) {
        String sql = """
                SELECT k.*, s.name as server_name, s.location as server_location FROM keys k
                JOIN users u ON u.id = k.user_id
                LEFT JOIN servers s ON s.id = k.server_id
                WHERE k.id = ? AND u.tg_id = ?
                """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, keyId);
            ps.setLong(2, tgId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("KeyDao.findByIdAndUserTgId failed", e);
        }
    }

    public static void updateTraffic(long keyId, long up, long down, long total, long expiryTs) {
        String sql = "UPDATE keys SET traffic_up=?, traffic_down=?, traffic_total=?, expiry_ts=? WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, up);
            ps.setLong(2, down);
            ps.setLong(3, total);
            ps.setLong(4, expiryTs);
            ps.setLong(5, keyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("KeyDao.updateTraffic failed", e);
        }
    }

    public static void resetTraffic(long keyId) {
        String sql = "UPDATE keys SET traffic_up=0, traffic_down=0 WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, keyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("KeyDao.resetTraffic failed", e);
        }
    }

    public static void delete(long keyId) {
        String sql = "DELETE FROM keys WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, keyId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("KeyDao.delete failed", e);
        }
    }

    public static long insert(Key key) {
        String sql = """
                INSERT INTO keys (user_id, server_id, inbound_id, xui_client_id, xui_email, remark, expiry_ts, traffic_total)
                VALUES (?,?,?,?,?,?,?,?)
                """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, key.userId);
            ps.setLong(2, key.serverId);
            ps.setInt(3, key.inboundId);
            ps.setString(4, key.xuiClientId);
            ps.setString(5, key.xuiEmail);
            ps.setString(6, key.remark);
            ps.setLong(7, key.expiryTs);
            ps.setLong(8, key.trafficTotal);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("KeyDao.insert failed", e);
        }
    }

    private static Key mapRow(ResultSet rs) throws SQLException {
        Key k = new Key();
        k.id = rs.getLong("id");
        k.userId = rs.getLong("user_id");
        k.serverId = rs.getLong("server_id");
        k.inboundId = rs.getInt("inbound_id");
        k.xuiClientId = rs.getString("xui_client_id");
        k.xuiEmail = rs.getString("xui_email");
        k.remark = rs.getString("remark");
        k.expiryTs = rs.getLong("expiry_ts");
        k.trafficTotal = rs.getLong("traffic_total");
        k.trafficUp = rs.getLong("traffic_up");
        k.trafficDown = rs.getLong("traffic_down");
        k.createdAt = rs.getString("created_at");
        return k;
    }
}
