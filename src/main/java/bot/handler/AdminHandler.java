package bot.handler;

import bot.db.Database;
import bot.keyboard.Menus;
import bot.service.XuiClient;
import com.fasterxml.jackson.databind.JsonNode;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AdminHandler {

    // FSM state per admin chat
    public enum State {
        IDLE,
        WAITING_TG_ID,
        WAITING_INBOUND,
        WAITING_EMAIL,
        WAITING_REMARK,
        WAITING_EXPIRY_DAYS,
        WAITING_TRAFFIC_GB
    }

    public static final Map<Long, State> adminState = new ConcurrentHashMap<>();
    public static final Map<Long, Map<String, Object>> adminData = new ConcurrentHashMap<>();

    public static void handlePanel(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text("⚙️ <b>Админка</b>\n\nДоступные команды:\n" +
                        "/add_key — добавить ключ пользователю\n" +
                        "/xui_inbounds — список inbound'ов с панели\n" +
                        "/users — список пользователей")
                .parseMode("HTML")
                .build());
    }

    public static void handleXuiInbounds(AbsSender bot, Message msg) throws TelegramApiException {
        XuiClient.get().login();
        List<JsonNode> inbounds = XuiClient.get().getInbounds();
        if (inbounds.isEmpty()) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId()).text("Панель недоступна или нет inbound'ов.").build());
            return;
        }
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text("Выбери inbound:")
                .replyMarkup(Menus.adminInboundsKeyboard(inbounds))
                .build());
    }

    public static void handleInboundDetail(AbsSender bot, CallbackQuery call, int inboundId) throws TelegramApiException {
        XuiClient.get().login();
        List<JsonNode> inbounds = XuiClient.get().getInbounds();
        JsonNode inbound = null;
        for (JsonNode ib : inbounds) {
            if (ib.path("id").asInt() == inboundId) { inbound = ib; break; }
        }
        if (inbound == null) {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text("Inbound не найден").showAlert(true).build());
            return;
        }
        List<JsonNode> clients = new java.util.ArrayList<>();
        inbound.path("clientStats").forEach(clients::add);
        StringBuilder sb = new StringBuilder("📡 <b>" +
                inbound.path("remark").asText(String.valueOf(inboundId)) + "</b> — " +
                clients.size() + " клиентов\n\n");
        int limit = Math.min(30, clients.size());
        for (int i = 0; i < limit; i++) {
            JsonNode cl = clients.get(i);
            double used = Math.round((cl.path("up").asLong(0) + cl.path("down").asLong(0))
                    / (1024.0 * 1024 * 1024) * 100.0) / 100.0;
            double total = Math.round(cl.path("total").asLong(0) / (1024.0 * 1024 * 1024) * 100.0) / 100.0;
            String en = cl.path("enable").asBoolean(false) ? "🟢" : "🔴";
            String totalStr = total == 0 ? "∞" : String.valueOf(total);
            sb.append(en).append(" <code>").append(cl.path("email").asText())
              .append("</code> — ").append(used).append("/").append(totalStr).append(" GB\n");
        }
        bot.execute(EditMessageText.builder()
                .chatId(call.getMessage().getChatId().toString())
                .messageId(call.getMessage().getMessageId())
                .text(sb.toString()).parseMode("HTML").build());
    }

    public static void handleAddKeyStart(AbsSender bot, Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        adminState.put(chatId, State.WAITING_TG_ID);
        adminData.put(chatId, new ConcurrentHashMap<>());
        bot.execute(SendMessage.builder().chatId(chatId).text("Telegram ID пользователя:").build());
    }

    public static void handleAddKeyListUsers(AbsSender bot, Message msg) throws TelegramApiException {
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement(
                "SELECT tg_id, username, full_name, balance, created_at FROM users ORDER BY created_at DESC LIMIT 30");
             ResultSet rs = ps.executeQuery()) {
            StringBuilder sb = new StringBuilder("<b>Пользователи</b>:\n\n");
            int count = 0;
            while (rs.next()) {
                count++;
                String name = rs.getString("full_name");
                if (name == null) name = rs.getString("username");
                if (name == null) name = String.valueOf(rs.getLong("tg_id"));
                sb.append("• <code>").append(rs.getLong("tg_id")).append("</code> ")
                  .append(name).append(" — ").append((int) rs.getDouble("balance")).append("⭐\n");
            }
            if (count == 0) { bot.execute(SendMessage.builder().chatId(msg.getChatId()).text("Пользователей нет.").build()); return; }
            bot.execute(SendMessage.builder().chatId(msg.getChatId()).text(sb.toString()).parseMode("HTML").build());
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    // FSM step handler — routes incoming text based on current state
    public static boolean handleFsmStep(AbsSender bot, Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        State state = adminState.getOrDefault(chatId, State.IDLE);
        if (state == State.IDLE) return false;

        Map<String, Object> data = adminData.computeIfAbsent(chatId, k -> new ConcurrentHashMap<>());
        String text = msg.getText() != null ? msg.getText().trim() : "";

        switch (state) {
            case WAITING_TG_ID -> {
                long tgId;
                try { tgId = Long.parseLong(text); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text("Нужен числовой ID.").build());
                    return true;
                }
                Connection c = Database.get();
                try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE tg_id=?")) {
                    ps.setLong(1, tgId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            bot.execute(SendMessage.builder().chatId(chatId).text("Пользователь не найден в БД.").build());
                            adminState.put(chatId, State.IDLE);
                            return true;
                        }
                        data.put("tg_id", tgId);
                        data.put("user_db_id", rs.getLong("id"));
                    }
                } catch (SQLException e) { throw new RuntimeException(e); }
                XuiClient.get().login();
                List<JsonNode> inbounds = XuiClient.get().getInbounds();
                if (inbounds.isEmpty()) {
                    bot.execute(SendMessage.builder().chatId(chatId).text("Не удалось получить inbound'ы с панели.").build());
                    adminState.put(chatId, State.IDLE);
                    return true;
                }
                data.put("inbounds", inbounds);
                StringBuilder sb = new StringBuilder("Inbound'ы:\n");
                for (JsonNode ib : inbounds)
                    sb.append("  ").append(ib.path("id").asInt()).append(" — ").append(ib.path("remark").asText("")).append("\n");
                sb.append("\nВведи ID inbound'а:");
                bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString()).build());
                adminState.put(chatId, State.WAITING_INBOUND);
            }
            case WAITING_INBOUND -> {
                try { data.put("inbound_id", Integer.parseInt(text)); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text("Числовой ID.").build());
                    return true;
                }
                bot.execute(SendMessage.builder().chatId(chatId).text("Email для клиента (уникальный на панели):").build());
                adminState.put(chatId, State.WAITING_EMAIL);
            }
            case WAITING_EMAIL -> {
                data.put("email", text);
                bot.execute(SendMessage.builder().chatId(chatId).text("Remark (описание, можно пустым):").build());
                adminState.put(chatId, State.WAITING_REMARK);
            }
            case WAITING_REMARK -> {
                data.put("remark", text);
                bot.execute(SendMessage.builder().chatId(chatId).text("Срок действия в днях (0 — бессрочно):").build());
                adminState.put(chatId, State.WAITING_EXPIRY_DAYS);
            }
            case WAITING_EXPIRY_DAYS -> {
                int days;
                try { days = Integer.parseInt(text); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text("Число.").build());
                    return true;
                }
                long expiryTs = days > 0 ? (System.currentTimeMillis() + (long) days * 86400 * 1000) : 0;
                data.put("expiry_ts", expiryTs);
                bot.execute(SendMessage.builder().chatId(chatId).text("Лимит трафика в GB (0 — безлимит):").build());
                adminState.put(chatId, State.WAITING_TRAFFIC_GB);
            }
            case WAITING_TRAFFIC_GB -> {
                int trafficGb;
                try { trafficGb = Integer.parseInt(text); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text("Число.").build());
                    return true;
                }
                adminState.put(chatId, State.IDLE);

                XuiClient.AddResult result = XuiClient.get().addClient(
                        (int) data.get("inbound_id"),
                        (String) data.get("email"),
                        (String) data.get("remark"),
                        (long) data.get("expiry_ts"),
                        trafficGb
                );
                if (!result.success()) {
                    bot.execute(SendMessage.builder().chatId(chatId).text("Ошибка при создании клиента на панели.").build());
                    return true;
                }
                long trafficBytes = trafficGb > 0 ? (long) trafficGb * 1024 * 1024 * 1024 : 0;
                Connection c = Database.get();
                try (PreparedStatement ps = c.prepareStatement("""
                        INSERT INTO keys (user_id, inbound_id, xui_client_id, xui_email, remark, expiry_ts, traffic_total)
                        VALUES (?,?,?,?,?,?,?)
                        """)) {
                    ps.setLong(1, (long) data.get("user_db_id"));
                    ps.setInt(2, (int) data.get("inbound_id"));
                    ps.setString(3, result.clientId());
                    ps.setString(4, (String) data.get("email"));
                    ps.setString(5, (String) data.get("remark"));
                    ps.setLong(6, (long) data.get("expiry_ts"));
                    ps.setLong(7, trafficBytes);
                    ps.executeUpdate();
                } catch (SQLException e) { throw new RuntimeException(e); }

                bot.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("✅ Ключ создан\n" +
                                "Email: <code>" + data.get("email") + "</code>\n" +
                                "UUID: <code>" + result.clientId() + "</code>")
                        .parseMode("HTML").build());
            }
        }
        return true;
    }
}
