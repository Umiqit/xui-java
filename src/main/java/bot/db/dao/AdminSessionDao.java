package bot.db.dao;

import bot.db.Database;
import bot.db.model.AdminSession;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class AdminSessionDao {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static AdminSession findByAdminTgId(long tgId) {
        String sql = "SELECT admin_tg_id, state, data, updated_at FROM admin_sessions WHERE admin_tg_id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tgId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new AdminSession(
                            rs.getLong("admin_tg_id"),
                            rs.getString("state"),
                            parseData(rs.getString("data")),
                            rs.getString("updated_at")
                    );
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("AdminSessionDao.findByAdminTgId failed", e);
        }
        return null;
    }

    public static void save(long tgId, String state, Map<String, Object> data) {
        String sql = """
                INSERT INTO admin_sessions (admin_tg_id, state, data)
                VALUES (?, ?, ?)
                ON CONFLICT(admin_tg_id) DO UPDATE SET
                    state=excluded.state,
                    data=excluded.data,
                    updated_at=CURRENT_TIMESTAMP
                """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tgId);
            ps.setString(2, state);
            ps.setString(3, serializeData(data));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("AdminSessionDao.save failed", e);
        }
    }

    public static void delete(long tgId) {
        String sql = "DELETE FROM admin_sessions WHERE admin_tg_id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, tgId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("AdminSessionDao.delete failed", e);
        }
    }

    private static Map<String, Object> parseData(String json) {
        try {
            if (json == null || json.isBlank()) return new HashMap<>();
            return MAPPER.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    private static String serializeData(Map<String, Object> data) {
        try {
            return MAPPER.writeValueAsString(data != null ? data : new HashMap<>());
        } catch (Exception e) {
            return "{}";
        }
    }
}
