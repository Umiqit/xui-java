package site.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.dto.TelegramAuthData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TelegramAuthService {

    private static final Logger log = LoggerFactory.getLogger(TelegramAuthService.class);

    @Value("${telegram.bot-token}")
    private String botToken;

    @PostConstruct
    public void init() {
        if (botToken == null || botToken.isBlank()) {
            System.err.println("[TelegramAuthService] BOT_TOKEN is not set!");
            log.error("BOT_TOKEN is not set! Telegram auth will fail.");
        } else {
            System.out.println("[TelegramAuthService] Initialized. Token length: " + botToken.length());
            log.info("TelegramAuthService initialized. Token length: {}", botToken.length());
        }
    }

    public boolean verify(TelegramAuthData data) {
        if (botToken == null || botToken.isBlank()) {
            log.error("Cannot verify Telegram auth: BOT_TOKEN is empty");
            return false;
        }

        System.out.println("[TelegramAuthService] Verifying auth for id=" + data.getId() +
                ", username=" + data.getUsername() + ", authDate=" + data.getAuthDate());
        log.info("Verifying Telegram auth for id={}, username={}, authDate={}",
                data.getId(), data.getUsername(), data.getAuthDate());

        Map<String, String> fields = new TreeMap<>();
        fields.put("auth_date", String.valueOf(data.getAuthDate()));
        if (data.getFirstName() != null && !data.getFirstName().isBlank()) {
            fields.put("first_name", data.getFirstName());
        }
        fields.put("id", String.valueOf(data.getId()));
        if (data.getLastName() != null && !data.getLastName().isBlank()) {
            fields.put("last_name", data.getLastName());
        }
        if (data.getPhotoUrl() != null && !data.getPhotoUrl().isBlank()) {
            fields.put("photo_url", data.getPhotoUrl());
        }
        if (data.getUsername() != null && !data.getUsername().isBlank()) {
            fields.put("username", data.getUsername());
        }

        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            if (!sb.isEmpty()) sb.append("\n");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
        }
        String dataCheckString = sb.toString();
        log.debug("dataCheckString: {}", dataCheckString);

        try {
            // Login Widget uses SHA256(botToken) as secret key
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] secretKeyBytes = digest.digest(botToken.getBytes(StandardCharsets.UTF_8));

            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
            mac.init(secretKey);
            byte[] hashBytes = mac.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            String computedHash = bytesToHex(hashBytes);
            System.out.println("[TelegramAuthService] Computed hash: " + computedHash +
                    ", Received hash: " + data.getHash());
            log.info("Computed hash: {}, Received hash: {}", computedHash, data.getHash());
            boolean ok = computedHash.equalsIgnoreCase(data.getHash());
            if (!ok) {
                System.out.println("[TelegramAuthService] Hash mismatch!");
                log.warn("Hash mismatch! Auth failed.");
            }
            return ok;
        } catch (Exception e) {
            log.error("Error verifying Telegram auth", e);
            return false;
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
