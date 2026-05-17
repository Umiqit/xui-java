package bot.db.model;

public class User {
    public long id;
    public long tgId;
    public String username;
    public String fullName;
    public double balance;
    public String createdAt;

    public User() {}

    public User(long id, long tgId, String username, String fullName, double balance, String createdAt) {
        this.id = id;
        this.tgId = tgId;
        this.username = username;
        this.fullName = fullName;
        this.balance = balance;
        this.createdAt = createdAt;
    }
}
