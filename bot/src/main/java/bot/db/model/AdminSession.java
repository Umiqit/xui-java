package bot.db.model;

import java.util.Map;

public class AdminSession {
    public long adminTgId;
    public String state;
    public Map<String, Object> data;
    public String updatedAt;

    public AdminSession() {}

    public AdminSession(long adminTgId, String state, Map<String, Object> data, String updatedAt) {
        this.adminTgId = adminTgId;
        this.state = state;
        this.data = data;
        this.updatedAt = updatedAt;
    }
}
