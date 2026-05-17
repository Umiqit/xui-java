package bot.db;

import bot.config.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static Connection conn;

    public static synchronized Connection get() {
        try {
            if (conn == null || conn.isClosed()) {
                String path = Settings.get().DB_PATH;
                conn = DriverManager.getConnection("jdbc:sqlite:" + path);
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB connection failed", e);
        }
        return conn;
    }

    public static void init() {
        Connection c = get();
        try (Statement st = c.createStatement()) {
            st.executeUpdate("PRAGMA journal_mode=WAL");
            st.executeUpdate("PRAGMA foreign_keys=ON");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    tg_id       INTEGER UNIQUE NOT NULL,
                    username    TEXT,
                    full_name   TEXT,
                    balance     REAL DEFAULT 0.0,
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS keys (
                    id              INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id         INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    inbound_id      INTEGER NOT NULL,
                    xui_client_id   TEXT NOT NULL,
                    xui_email       TEXT NOT NULL,
                    remark          TEXT DEFAULT '',
                    expiry_ts       INTEGER DEFAULT 0,
                    traffic_total   INTEGER DEFAULT 0,
                    traffic_up      INTEGER DEFAULT 0,
                    traffic_down    INTEGER DEFAULT 0,
                    created_at      DATETIME DEFAULT CURRENT_TIMESTAMP
                )""");

            st.executeUpdate("""
                CREATE TABLE IF NOT EXISTS payments (
                    id          INTEGER PRIMARY KEY AUTOINCREMENT,
                    user_id     INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    amount      REAL NOT NULL,
                    currency    TEXT DEFAULT 'XTR',
                    payload     TEXT,
                    status      TEXT DEFAULT 'pending',
                    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                )""");

            log.info("DB initialized");
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    public static void close() {
        if (conn != null) {
            try { conn.close(); } catch (SQLException ignored) {}
            conn = null;
        }
    }
}
