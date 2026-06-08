package bot.db.model;

public class Order {
    public long id;
    public long userId;
    public long productId;
    public Long keyId;
    public double amount;
    public String status;
    public String createdAt;
    public String productName;

    public Order() {}

    public Order(long id, long userId, long productId, Long keyId, double amount, String status, String createdAt, String productName) {
        this.id = id;
        this.userId = userId;
        this.productId = productId;
        this.keyId = keyId;
        this.amount = amount;
        this.status = status;
        this.createdAt = createdAt;
        this.productName = productName;
    }
}
