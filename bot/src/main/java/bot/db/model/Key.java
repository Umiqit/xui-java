package bot.db.model;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class Key {
    public long id;
    public long userId;
    public long serverId;
    public int inboundId;
    public String xuiClientId;
    public String xuiEmail;
    public String remark;
    public long expiryTs;
    public long trafficTotal;
    public long trafficUp;
    public long trafficDown;
    public String createdAt;

    public Key() {}

    public double trafficUsedGb() {
        return Math.round((trafficUp + trafficDown) / (1024.0 * 1024 * 1024) * 100.0) / 100.0;
    }

    public double trafficTotalGb() {
        if (trafficTotal == 0) return 0;
        return Math.round(trafficTotal / (1024.0 * 1024 * 1024) * 100.0) / 100.0;
    }

    public String expiryDate() {
        if (expiryTs == 0) return "Бессрочно";
        LocalDateTime dt = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(expiryTs), ZoneId.systemDefault());
        return dt.format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm"));
    }

    public boolean isExpired() {
        if (expiryTs == 0) return false;
        return expiryTs < System.currentTimeMillis();
    }
}
