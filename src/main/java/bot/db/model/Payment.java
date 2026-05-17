package bot.db.model;

public class Payment {
    public long id;
    public long userId;
    public double amount;
    public String currency;
    public String payload;
    public String status;
    public String createdAt;

    public Payment() {}
}
