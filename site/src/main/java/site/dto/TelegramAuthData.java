package site.dto;

public class TelegramAuthData {
    private long id;
    private String firstName;
    private String lastName;
    private String username;
    private String photoUrl;
    private long authDate;
    private String hash;

    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public long getAuthDate() { return authDate; }
    public void setAuthDate(long authDate) { this.authDate = authDate; }

    public String getHash() { return hash; }
    public void setHash(String hash) { this.hash = hash; }
}
