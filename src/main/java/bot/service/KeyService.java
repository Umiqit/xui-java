package bot.service;

import bot.db.dao.KeyDao;
import bot.db.model.Key;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class KeyService {

    public static List<Key> getUserKeys(long tgId) {
        return KeyDao.findByUserTgId(tgId);
    }

    public static Key getKeyDetail(long keyId, long tgId) {
        Key key = KeyDao.findByIdAndUserTgId(keyId, tgId);
        if (key == null) return null;
        refreshKeyStats(key);
        return key;
    }

    public static void refreshAllUserKeys(long tgId) {
        List<Key> keys = KeyDao.findByUserTgId(tgId);
        for (Key k : keys) {
            refreshKeyStats(k);
        }
    }

    public static void resetTraffic(long keyId, long tgId) {
        Key key = KeyDao.findByIdAndUserTgId(keyId, tgId);
        if (key == null) throw new IllegalArgumentException("Key not found");
        boolean ok = XuiClient.get().resetClientTraffic(key.inboundId, key.xuiEmail);
        if (!ok) throw new XuiApiException("Panel returned failure for reset traffic");
        KeyDao.resetTraffic(keyId);
    }

    public static void deleteKey(long keyId, long tgId) {
        Key key = KeyDao.findByIdAndUserTgId(keyId, tgId);
        if (key == null) throw new IllegalArgumentException("Key not found");
        boolean ok = XuiClient.get().deleteClient(key.inboundId, key.xuiClientId);
        if (!ok) throw new XuiApiException("Panel returned failure for delete client");
        KeyDao.delete(keyId);
    }

    private static void refreshKeyStats(Key key) {
        try {
            JsonNode stats = XuiClient.get().getClientStats(key.xuiEmail);
            if (stats != null) {
                long up = stats.path("up").asLong(0);
                long down = stats.path("down").asLong(0);
                long total = stats.path("total").asLong(0);
                long expiry = stats.path("expiryTime").asLong(0);
                KeyDao.updateTraffic(key.id, up, down, total, expiry);
                key.trafficUp = up;
                key.trafficDown = down;
                key.trafficTotal = total;
                key.expiryTs = expiry;
            }
        } catch (XuiApiException e) {
            // leave local stats unchanged if panel is down
        }
    }
}
