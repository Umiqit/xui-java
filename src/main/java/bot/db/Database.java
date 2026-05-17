package bot.db;

import bot.config.Settings;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final Logger log = LoggerFactory.getLogger(Database.class);
    private static HikariDataSource ds;

    public static void init() {
        String type = Settings.get().DB_TYPE;
        if ("postgres".equals(type)) {
            String url = String.format("jdbc:postgresql://%s:%s/%s",
                    Settings.get().DB_HOST,
                    Settings.get().DB_PORT,
                    Settings.get().DB_NAME);
            initPostgres(url);
        } else {
            initWithUrl("jdbc:sqlite:" + Settings.get().DB_PATH);
        }
    }

    public static void initWithUrl(String jdbcUrl) {
        boolean isPostgres = jdbcUrl.startsWith("jdbc:postgresql");
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setMaximumPoolSize(isPostgres ? 10 : 4);
        config.setConnectionTestQuery("SELECT 1");
        if (isPostgres) {
            config.setDriverClassName("org.postgresql.Driver");
        } else {
            config.setDriverClassName("org.sqlite.JDBC");
        }
        ds = new HikariDataSource(config);

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
            if (!isPostgres) {
                st.executeUpdate("PRAGMA journal_mode=WAL");
                st.executeUpdate("PRAGMA foreign_keys=ON");
            }

            if (isPostgres) {
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
            } else {
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

                st.executeUpdate("""
                    CREATE TABLE IF NOT EXISTS admin_sessions (
                        admin_tg_id INTEGER PRIMARY KEY,
                        state       TEXT NOT NULL,
                        data        TEXT DEFAULT '{}',
                        updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP
                    )""");
            }

            log.info("DB initialized ({})", isPostgres ? "postgres" : "sqlite");
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    private static void initPostgres(String jdbcUrl) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(Settings.get().DB_USER);
        config.setPassword(Settings.get().DB_PASSWORD);
        config.setMaximumPoolSize(10);
        config.setConnectionTestQuery("SELECT 1");
        config.setDriverClassName("org.postgresql.Driver");
        ds = new HikariDataSource(config);

        try (Connection c = ds.getConnection(); Statement st = c.createStatement()) {
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

            log.info("DB initialized (postgres)");
        } catch (SQLException e) {
            throw new RuntimeException("DB init failed", e);
        }
    }

    public static Connection get() throws SQLException {
        if (ds == null) {
            throw new IllegalStateException("Database not initialized");
        }
        return ds.getConnection();
    }

    public static void close() {
        if (ds != null && !ds.isClosed()) {
            ds.close();
            log.info("DB pool closed");
        }
    }
}
