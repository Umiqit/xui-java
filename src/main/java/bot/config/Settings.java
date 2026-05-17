package bot.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.ArrayList;
import java.util.List;

public class Settings {

    private static final Settings INSTANCE = new Settings();
    private final Dotenv env;

    public final String BOT_TOKEN;
    public final String BOT_USERNAME;
    public final List<Long> ADMIN_IDS;
    public final String XUI_URL;
    public final String XUI_USERNAME;
    public final String XUI_PASSWORD;
    public final String XUI_CERT_PATH;
    public final String DB_TYPE;
    public final String DB_PATH;
    public final String DB_HOST;
    public final String DB_PORT;
    public final String DB_NAME;
    public final String DB_USER;
    public final String DB_PASSWORD;

    private Settings() {
        env = Dotenv.configure().ignoreIfMissing().load();
        BOT_TOKEN    = require("BOT_TOKEN");
        BOT_USERNAME = require("BOT_USERNAME");
        XUI_URL      = require("XUI_URL");
        XUI_USERNAME = require("XUI_USERNAME");
        XUI_PASSWORD = require("XUI_PASSWORD");
        XUI_CERT_PATH = get("XUI_CERT_PATH", "");

        DB_TYPE = get("DB_TYPE", "sqlite").toLowerCase();
        if (DB_TYPE.equals("postgres")) {
            DB_HOST = require("DB_HOST");
            DB_PORT = get("DB_PORT", "5432");
            DB_NAME = require("DB_NAME");
            DB_USER = require("DB_USER");
            DB_PASSWORD = require("DB_PASSWORD");
            DB_PATH = "";
        } else {
            DB_PATH = get("DB_PATH", "bot.db");
            DB_HOST = "";
            DB_PORT = "";
            DB_NAME = "";
            DB_USER = "";
            DB_PASSWORD = "";
        }

        String raw = get("ADMIN_IDS", "");
        ADMIN_IDS = parseAdminIds(raw);
    }

    private String require(String key) {
        String v = env.get(key);
        if (v == null || v.isBlank())
            throw new IllegalArgumentException("Missing required env var: " + key);
        return v;
    }

    private String get(String key, String def) {
        String v = env.get(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    private static List<Long> parseAdminIds(String raw) {
        if (raw == null || raw.isBlank()) return List.of();
        List<Long> ids = new ArrayList<>();
        for (String s : raw.split(",")) {
            s = s.trim();
            if (s.isEmpty()) continue;
            try {
                ids.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(
                        "ADMIN_IDS contains invalid value: '" + s + "'. Expected comma-separated numbers.");
            }
        }
        return List.copyOf(ids);
    }

    public static Settings get() { return INSTANCE; }
}
