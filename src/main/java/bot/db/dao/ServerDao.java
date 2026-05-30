package bot.db.dao;

import bot.db.Database;
import bot.db.model.Server;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServerDao {

    public static List<Server> findAll() {
        String sql = "SELECT * FROM servers ORDER BY id";
        List<Server> list = new ArrayList<>();
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ServerDao.findAll failed", e);
        }
        return list;
    }

    public static List<Server> findActive() {
        String sql = "SELECT * FROM servers WHERE active = 1 ORDER BY id";
        List<Server> list = new ArrayList<>();
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new RuntimeException("ServerDao.findActive failed", e);
        }
        return list;
    }

    public static Server findById(long id) {
        String sql = "SELECT * FROM servers WHERE id = ?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapRow(rs) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ServerDao.findById failed", e);
        }
    }

    public static long insert(Server s) {
        String sql = """
            INSERT INTO servers (name, location, url, username, password, cert_path, active, weight)
            VALUES (?,?,?,?,?,?,?,?)
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.name);
            ps.setString(2, s.location);
            ps.setString(3, s.url);
            ps.setString(4, s.username);
            ps.setString(5, s.password);
            ps.setString(6, s.certPath);
            ps.setBoolean(7, s.active);
            ps.setInt(8, s.weight);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                return rs.next() ? rs.getLong(1) : -1;
            }
        } catch (SQLException e) {
            throw new RuntimeException("ServerDao.insert failed", e);
        }
    }

    public static void update(Server s) {
        String sql = """
            UPDATE servers SET name=?, location=?, url=?, username=?, password=?,
            cert_path=?, active=?, weight=? WHERE id=?
            """;
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, s.name);
            ps.setString(2, s.location);
            ps.setString(3, s.url);
            ps.setString(4, s.username);
            ps.setString(5, s.password);
            ps.setString(6, s.certPath);
            ps.setBoolean(7, s.active);
            ps.setInt(8, s.weight);
            ps.setLong(9, s.id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ServerDao.update failed", e);
        }
    }

    public static void delete(long id) {
        String sql = "DELETE FROM servers WHERE id=?";
        try (Connection c = Database.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("ServerDao.delete failed", e);
        }
    }

    public static Server findFirst() {
        String sql = "SELECT * FROM servers ORDER BY id ASC LIMIT 1";
        try (Connection c = Database.get();
             Statement st = c.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            return rs.next() ? mapRow(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException("ServerDao.findFirst failed", e);
        }
    }

    private static Server mapRow(ResultSet rs) throws SQLException {
        Server s = new Server();
        s.id = rs.getLong("id");
        s.name = rs.getString("name");
        s.location = rs.getString("location");
        s.url = rs.getString("url");
        s.username = rs.getString("username");
        s.password = rs.getString("password");
        s.certPath = rs.getString("cert_path");
        s.active = rs.getBoolean("active");
        s.weight = rs.getInt("weight");
        s.createdAt = rs.getString("created_at");
        return s;
    }
}
