package site.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class TelegramAuthData {
    private long id;

    @JsonProperty("first_name")
    private String firstName;

    @JsonProperty("last_name")
    private String lastName;

    private String username;

    @JsonProperty("photo_url")
    private String photoUrl;

    @JsonProperty("auth_date")
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

    @Override
    public String toString() {
        return "TelegramAuthData{id=" + id + ", username='" + username + "', authDate=" + authDate + "}";
    }
}
