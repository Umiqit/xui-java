package bot.db.model;

public class Server {
    public long id;
    public String name;
    public String location;
    public String url;
    public String username;
    public String password;
    public String certPath;
    public boolean active;
    public int weight;
    public String createdAt;

    public Server() {}

    public String displayName() {
        if (name != null && !name.isBlank()) return name;
        return "Server " + id;
    }

    public String displayLocation() {
        if (location != null && !location.isBlank()) return location;
        return "—";
    }
}
