package bot.config;

import io.github.cdimascio.dotenv.Dotenv;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class Settings {

    private static final Settings INSTANCE = new Settings();
    private final Dotenv env;

    public final String BOT_TOKEN;
    public final String BOT_USERNAME;
    public final List<Long> ADMIN_IDS;
    public final String XUI_URL;
    public final String XUI_USERNAME;
    public final String XUI_PASSWORD;
    public final String DB_PATH;

    private Settings() {
        env = Dotenv.configure().ignoreIfMissing().load();
        BOT_TOKEN    = require("BOT_TOKEN");
        BOT_USERNAME = require("BOT_USERNAME");
        XUI_URL      = require("XUI_URL");
        XUI_USERNAME = require("XUI_USERNAME");
        XUI_PASSWORD = require("XUI_PASSWORD");
        DB_PATH      = get("DB_PATH", "bot.db");

        String raw = get("ADMIN_IDS", "");
        ADMIN_IDS = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(Long::parseLong)
                .collect(Collectors.toList());
    }

    private String require(String key) {
        String v = env.get(key);
        if (v == null || v.isBlank())
            throw new RuntimeException("Missing required env var: " + key);
        return v;
    }

    private String get(String key, String def) {
        String v = env.get(key);
        return (v == null || v.isBlank()) ? def : v;
    }

    public static Settings get() { return INSTANCE; }
}
