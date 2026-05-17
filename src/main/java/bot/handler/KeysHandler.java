package bot.handler;

import bot.db.Database;
import bot.db.model.Key;
import bot.keyboard.Menus;
import bot.service.XuiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class KeysHandler {

    private static List<Map<String, Object>> getUserKeys(long tgId) throws SQLException {
        Connection c = Database.get();
        List<Map<String, Object>> result = new ArrayList<>();
        try (PreparedStatement ps = c.prepareStatement("""
                SELECT k.* FROM keys k
                JOIN users u ON u.id = k.user_id
                WHERE u.tg_id = ?
                ORDER BY k.created_at DESC
                """)) {
            ps.setLong(1, tgId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Key k = mapRow(rs);
                    Map<String, Object> m = new HashMap<>();
                    m.put("id", k.id);
                    m.put("remark", k.remark);
                    m.put("xui_email", k.xuiEmail);
                    m.put("traffic_used_gb", k.trafficUsedGb());
                    m.put("traffic_total_gb", k.trafficTotalGb());
                    m.put("expiry_date", k.expiryDate());
                    m.put("is_expired", k.isExpired());
                    m.put("inbound_id", k.inboundId);
                    m.put("xui_client_id", k.xuiClientId);
                    result.add(m);
                }
            }
        }
        return result;
    }

    private static Key mapRow(ResultSet rs) throws SQLException {
        Key k = new Key();
        k.id           = rs.getLong("id");
        k.userId       = rs.getLong("user_id");
        k.inboundId    = rs.getInt("inbound_id");
        k.xuiClientId  = rs.getString("xui_client_id");
        k.xuiEmail     = rs.getString("xui_email");
        k.remark       = rs.getString("remark");
        k.expiryTs     = rs.getLong("expiry_ts");
        k.trafficTotal = rs.getLong("traffic_total");
        k.trafficUp    = rs.getLong("traffic_up");
        k.trafficDown  = rs.getLong("traffic_down");
        k.createdAt    = rs.getString("created_at");
        return k;
    }

    public static void handleList(AbsSender bot, Message msg) throws TelegramApiException {
        try {
            List<Map<String, Object>> keys = getUserKeys(msg.getFrom().getId());
            if (keys.isEmpty()) {
                bot.execute(SendMessage.builder()
                        .chatId(msg.getChatId()).text("У тебя пока нет ключей.").build());
                return;
            }
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text("🔑 <b>Твои ключи</b> (" + keys.size() + "):")
                    .parseMode("HTML")
                    .replyMarkup(Menus.keysKeyboard(keys))
                    .build());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleKeysList(AbsSender bot, CallbackQuery call) throws TelegramApiException {
        try {
            List<Map<String, Object>> keys = getUserKeys(call.getFrom().getId());
            String chatId = call.getMessage().getChatId().toString();
            int msgId = call.getMessage().getMessageId();
            if (keys.isEmpty()) {
                bot.execute(EditMessageText.builder()
                        .chatId(chatId).messageId(msgId).text("У тебя пока нет ключей.").build());
                return;
            }
            bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(msgId)
                    .text("🔑 <b>Твои ключи</b> (" + keys.size() + "):")
                    .parseMode("HTML")
                    .replyMarkup(Menus.keysKeyboard(keys))
                    .build());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleRefresh(AbsSender bot, CallbackQuery call) throws TelegramApiException {
        try {
            Connection c = Database.get();
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT k.* FROM keys k JOIN users u ON u.id=k.user_id WHERE u.tg_id=?")) {
                ps.setLong(1, call.getFrom().getId());
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Key k = mapRow(rs);
                        JsonNode stats = XuiClient.get().getClientStats(k.xuiEmail);
                        if (stats != null) {
                            try (PreparedStatement upd = c.prepareStatement(
                                    "UPDATE keys SET traffic_up=?,traffic_down=?,traffic_total=?,expiry_ts=? WHERE id=?")) {
                                upd.setLong(1, stats.path("up").asLong(0));
                                upd.setLong(2, stats.path("down").asLong(0));
                                upd.setLong(3, stats.path("total").asLong(0));
                                upd.setLong(4, stats.path("expiryTime").asLong(0));
                                upd.setLong(5, k.id);
                                upd.executeUpdate();
                            }
                        }
                    }
                }
            }
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text("Обновлено").build());
            List<Map<String, Object>> keys = getUserKeys(call.getFrom().getId());
            bot.execute(EditMessageReplyMarkup.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .replyMarkup(Menus.keysKeyboard(keys))
                    .build());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleDetail(AbsSender bot, CallbackQuery call, long keyId) throws TelegramApiException {
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT k.* FROM keys k JOIN users u ON u.id=k.user_id WHERE k.id=? AND u.tg_id=?")) {
            ps.setLong(1, keyId);
            ps.setLong(2, call.getFrom().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    bot.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(call.getId()).text("Ключ не найден").showAlert(true).build());
                    return;
                }
                Key k = mapRow(rs);
                JsonNode stats = XuiClient.get().getClientStats(k.xuiEmail);
                if (stats != null) {
                    try (PreparedStatement upd = c.prepareStatement(
                            "UPDATE keys SET traffic_up=?,traffic_down=?,traffic_total=?,expiry_ts=? WHERE id=?")) {
                        upd.setLong(1, stats.path("up").asLong(0));
                        upd.setLong(2, stats.path("down").asLong(0));
                        upd.setLong(3, stats.path("total").asLong(0));
                        upd.setLong(4, stats.path("expiryTime").asLong(0));
                        upd.setLong(5, k.id);
                        upd.executeUpdate();
                    }
                    k.trafficUp    = stats.path("up").asLong(0);
                    k.trafficDown  = stats.path("down").asLong(0);
                    k.trafficTotal = stats.path("total").asLong(0);
                    k.expiryTs     = stats.path("expiryTime").asLong(0);
                }
                String status = k.isExpired() ? "🔴 Истёк" : "🟢 Активен";
                String totalStr = k.trafficTotalGb() == 0 ? "∞" : String.valueOf(k.trafficTotalGb());
                String text = "🔑 <b>" + (k.remark != null && !k.remark.isBlank() ? k.remark : k.xuiEmail) + "</b>\n\n" +
                        "Статус: " + status + "\n" +
                        "Email (XUI): <code>" + k.xuiEmail + "</code>\n" +
                        "Inbound ID: " + k.inboundId + "\n\n" +
                        "📊 Трафик: " + k.trafficUsedGb() + " / " + totalStr + " GB\n" +
                        "📅 Истекает: " + k.expiryDate();
                bot.execute(EditMessageText.builder()
                        .chatId(call.getMessage().getChatId().toString())
                        .messageId(call.getMessage().getMessageId())
                        .text(text).parseMode("HTML")
                        .replyMarkup(Menus.keyDetailKeyboard(k.id))
                        .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleResetTraffic(AbsSender bot, CallbackQuery call, long keyId) throws TelegramApiException {
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT k.* FROM keys k JOIN users u ON u.id=k.user_id WHERE k.id=? AND u.tg_id=?")) {
            ps.setLong(1, keyId);
            ps.setLong(2, call.getFrom().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    bot.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(call.getId()).text("Ключ не найден").showAlert(true).build());
                    return;
                }
                Key k = mapRow(rs);
                boolean ok = XuiClient.get().resetClientTraffic(k.inboundId, k.xuiEmail);
                if (ok) {
                    try (PreparedStatement upd = c.prepareStatement(
                            "UPDATE keys SET traffic_up=0, traffic_down=0 WHERE id=?")) {
                        upd.setLong(1, keyId);
                        upd.executeUpdate();
                    }
                    bot.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(call.getId()).text("Трафик сброшен").showAlert(true).build());
                } else {
                    bot.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(call.getId()).text("Ошибка при сбросе трафика").showAlert(true).build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handleDelete(AbsSender bot, CallbackQuery call, long keyId) throws TelegramApiException {
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT k.* FROM keys k JOIN users u ON u.id=k.user_id WHERE k.id=? AND u.tg_id=?")) {
            ps.setLong(1, keyId);
            ps.setLong(2, call.getFrom().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    bot.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(call.getId()).text("Ключ не найден").showAlert(true).build());
                    return;
                }
                Key k = mapRow(rs);
                boolean ok = XuiClient.get().deleteClient(k.inboundId, k.xuiClientId);
                if (ok) {
                    try (PreparedStatement del = c.prepareStatement("DELETE FROM keys WHERE id=?")) {
                        del.setLong(1, keyId);
                        del.executeUpdate();
                    }
                    bot.execute(EditMessageText.builder()
                            .chatId(call.getMessage().getChatId().toString())
                            .messageId(call.getMessage().getMessageId())
                            .text("Ключ удалён.").build());
                } else {
                    bot.execute(AnswerCallbackQuery.builder()
                            .callbackQueryId(call.getId()).text("Ошибка при удалении с панели").showAlert(true).build());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}
