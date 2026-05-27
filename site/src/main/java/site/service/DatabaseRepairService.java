package site.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.*;

@Service
public class DatabaseRepairService {

    @Autowired
    private DataSource dataSource;

    public void repair() throws SQLException {
        try (Connection c = dataSource.getConnection(); Statement st = c.createStatement()) {
            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id          BIGSERIAL PRIMARY KEY,
                    tg_id       BIGINT UNIQUE NOT NULL,
                    username    TEXT,
                    full_name   TEXT,
                    balance     DOUBLE PRECISION DEFAULT 0.0,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS keys (
                    id              BIGSERIAL PRIMARY KEY,
                    user_id         BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    inbound_id      INTEGER NOT NULL,
                    xui_client_id   TEXT NOT NULL,
                    xui_email       TEXT NOT NULL,
                    remark          TEXT DEFAULT '',
                    expiry_ts       BIGINT DEFAULT 0,
                    traffic_total   BIGINT DEFAULT 0,
                    traffic_up      BIGINT DEFAULT 0,
                    traffic_down    BIGINT DEFAULT 0,
                    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS payments (
                    id          BIGSERIAL PRIMARY KEY,
                    user_id     BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    amount      DOUBLE PRECISION NOT NULL,
                    currency    TEXT DEFAULT 'XTR',
                    payload     TEXT,
                    status      TEXT DEFAULT 'pending',
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS admin_sessions (
                    admin_tg_id BIGINT PRIMARY KEY,
                    state       TEXT NOT NULL,
                    data        TEXT DEFAULT '{}',
                    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                )""");

            addColumnIfNotExists(c, "users", "tg_id", "BIGINT NOT NULL");
            addColumnIfNotExists(c, "users", "username", "TEXT");
            addColumnIfNotExists(c, "users", "full_name", "TEXT");
            addColumnIfNotExists(c, "users", "balance", "DOUBLE PRECISION DEFAULT 0.0");
            addColumnIfNotExists(c, "users", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            addColumnIfNotExists(c, "keys", "user_id", "BIGINT NOT NULL");
            addColumnIfNotExists(c, "keys", "inbound_id", "INTEGER NOT NULL");
            addColumnIfNotExists(c, "keys", "xui_client_id", "TEXT NOT NULL");
            addColumnIfNotExists(c, "keys", "xui_email", "TEXT NOT NULL");
            addColumnIfNotExists(c, "keys", "remark", "TEXT DEFAULT ''");
            addColumnIfNotExists(c, "keys", "expiry_ts", "BIGINT DEFAULT 0");
            addColumnIfNotExists(c, "keys", "traffic_total", "BIGINT DEFAULT 0");
            addColumnIfNotExists(c, "keys", "traffic_up", "BIGINT DEFAULT 0");
            addColumnIfNotExists(c, "keys", "traffic_down", "BIGINT DEFAULT 0");
            addColumnIfNotExists(c, "keys", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            addColumnIfNotExists(c, "payments", "user_id", "BIGINT NOT NULL");
            addColumnIfNotExists(c, "payments", "amount", "DOUBLE PRECISION NOT NULL");
            addColumnIfNotExists(c, "payments", "currency", "TEXT DEFAULT 'XTR'");
            addColumnIfNotExists(c, "payments", "payload", "TEXT");
            addColumnIfNotExists(c, "payments", "status", "TEXT DEFAULT 'pending'");
            addColumnIfNotExists(c, "payments", "created_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");

            addColumnIfNotExists(c, "admin_sessions", "state", "TEXT NOT NULL");
            addColumnIfNotExists(c, "admin_sessions", "data", "TEXT DEFAULT '{}'");
            addColumnIfNotExists(c, "admin_sessions", "updated_at", "TIMESTAMP DEFAULT CURRENT_TIMESTAMP");
        }
    }

    private void addColumnIfNotExists(Connection c, String table, String column, String def) throws SQLException {
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT 1 FROM information_schema.columns WHERE table_name = ? AND column_name = ?")) {
            ps.setString(1, table);
            ps.setString(2, column);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                try (Statement st = c.createStatement()) {
                    st.executeUpdate("ALTER TABLE " + table + " ADD COLUMN " + column + " " + def);
                }
            }
        }
    }
}
