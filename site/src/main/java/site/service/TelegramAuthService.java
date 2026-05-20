package site.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.dto.TelegramAuthData;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

@Service
public class TelegramAuthService {

    @Value("${telegram.bot-token}")
    private String botToken;

    public boolean verify(TelegramAuthData data) {
        if (botToken == null || botToken.isBlank()) {
            return false;
        }

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

        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey = new SecretKeySpec(botToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
            mac.init(secretKey);
            byte[] secretKeyBytes = mac.doFinal("WebAppData".getBytes(StandardCharsets.UTF_8));

            Mac mac2 = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKey2 = new SecretKeySpec(secretKeyBytes, "HmacSHA256");
            mac2.init(secretKey2);
            byte[] hashBytes = mac2.doFinal(dataCheckString.getBytes(StandardCharsets.UTF_8));

            String computedHash = bytesToHex(hashBytes);
            return computedHash.equalsIgnoreCase(data.getHash());
        } catch (Exception e) {
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
