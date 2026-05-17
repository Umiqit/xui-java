package bot.handler;

import bot.db.dao.AdminSessionDao;
import bot.db.dao.KeyDao;
import bot.db.dao.UserDao;
import bot.db.model.AdminSession;
import bot.db.model.Key;
import bot.keyboard.Menus;
import bot.service.AdminService;
import bot.service.XuiClient;
import bot.service.XuiApiException;
import bot.util.Messages;
import com.fasterxml.jackson.databind.JsonNode;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminHandler {

    public enum State {
        IDLE,
        WAITING_TG_ID,
        WAITING_INBOUND,
        WAITING_EMAIL,
        WAITING_REMARK,
        WAITING_EXPIRY_DAYS,
        WAITING_TRAFFIC_GB
    }

    public static void handlePanel(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.ADMIN_PANEL)
                .parseMode("HTML")
                .build());
    }

    public static void handleXuiInbounds(AbsSender bot, Message msg) throws TelegramApiException {
        try {
            XuiClient.get().login();
            List<JsonNode> inbounds = XuiClient.get().getInbounds();
            if (inbounds.isEmpty()) {
                bot.execute(SendMessage.builder()
                        .chatId(msg.getChatId()).text(Messages.XUI_NO_INBOUNDS).build());
                return;
            }
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text("Выбери inbound:")
                    .replyMarkup(Menus.adminInboundsKeyboard(inbounds))
                    .build());
        } catch (XuiApiException e) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId()).text(Messages.SERVICE_UNAVAILABLE).build());
        }
    }

    public static void handleInboundDetail(AbsSender bot, CallbackQuery call, int inboundId) throws TelegramApiException {
        try {
            XuiClient.get().login();
            List<JsonNode> inbounds = XuiClient.get().getInbounds();
            JsonNode inbound = null;
            for (JsonNode ib : inbounds) {
                if (ib.path("id").asInt() == inboundId) { inbound = ib; break; }
            }
            if (inbound == null) {
                bot.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(call.getId()).text(Messages.XUI_INBOUND_NOT_FOUND).showAlert(true).build());
                return;
            }
            String text = AdminService.formatInboundDetail(inbound);
            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(text).parseMode("HTML").build());
        } catch (XuiApiException e) {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.SERVICE_UNAVAILABLE).showAlert(true).build());
        }
    }

    public static void handleAddKeyStart(AbsSender bot, Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        AdminSessionDao.save(chatId, State.WAITING_TG_ID.name(), new HashMap<>());
        bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_ENTER_TG_ID).build());
    }

    public static void cancel(AbsSender bot, Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        AdminSessionDao.delete(chatId);
        bot.execute(SendMessage.builder().chatId(chatId).text(Messages.CANCEL_OK).build());
    }

    public static void handleAddKeyListUsers(AbsSender bot, Message msg) throws TelegramApiException {
        String text = AdminService.formatUsersList();
        if (text == null) {
            bot.execute(SendMessage.builder().chatId(msg.getChatId()).text(Messages.NO_USERS).build());
            return;
        }
        bot.execute(SendMessage.builder().chatId(msg.getChatId()).text(text).parseMode("HTML").build());
    }

    public static boolean handleFsmStep(AbsSender bot, Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        AdminSession session = AdminSessionDao.findByAdminTgId(chatId);
        if (session == null) return false;

        State state;
        try { state = State.valueOf(session.state); }
        catch (IllegalArgumentException e) {
            AdminSessionDao.delete(chatId);
            return false;
        }
        if (state == State.IDLE) return false;

        Map<String, Object> data = session.data != null ? session.data : new HashMap<>();
        String text = msg.getText() != null ? msg.getText().trim() : "";

        switch (state) {
            case WAITING_TG_ID -> {
                long tgId;
                try { tgId = Long.parseLong(text); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_NEED_NUMBER).build());
                    return true;
                }
                Long dbId = UserDao.getIdByTgId(tgId);
                if (dbId == null) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_USER_NOT_FOUND).build());
                    AdminSessionDao.delete(chatId);
                    return true;
                }
                data.put("tg_id", tgId);
                data.put("user_db_id", dbId);

                try {
                    XuiClient.get().login();
                    List<JsonNode> inbounds = XuiClient.get().getInbounds();
                    if (inbounds.isEmpty()) {
                        bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_NO_INBOUNDS).build());
                        AdminSessionDao.delete(chatId);
                        return true;
                    }
                    StringBuilder sb = new StringBuilder("Inbound'ы:\n");
                    for (JsonNode ib : inbounds)
                        sb.append("  ").append(ib.path("id").asInt()).append(" — ").append(ib.path("remark").asText("")).append("\n");
                    sb.append(Messages.ADD_KEY_ENTER_INBOUND);
                    bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString()).build());
                    AdminSessionDao.save(chatId, State.WAITING_INBOUND.name(), data);
                } catch (XuiApiException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.SERVICE_UNAVAILABLE).build());
                    AdminSessionDao.delete(chatId);
                }
            }
            case WAITING_INBOUND -> {
                try { data.put("inbound_id", Integer.parseInt(text)); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_NEED_INT).build());
                    return true;
                }
                bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_ENTER_EMAIL).build());
                AdminSessionDao.save(chatId, State.WAITING_EMAIL.name(), data);
            }
            case WAITING_EMAIL -> {
                data.put("email", text);
                bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_ENTER_REMARK).build());
                AdminSessionDao.save(chatId, State.WAITING_REMARK.name(), data);
            }
            case WAITING_REMARK -> {
                data.put("remark", text);
                bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_ENTER_EXPIRY).build());
                AdminSessionDao.save(chatId, State.WAITING_EXPIRY_DAYS.name(), data);
            }
            case WAITING_EXPIRY_DAYS -> {
                int days;
                try { days = Integer.parseInt(text); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_NEED_INT).build());
                    return true;
                }
                long expiryTs = days > 0 ? (System.currentTimeMillis() + (long) days * 86400 * 1000) : 0;
                data.put("expiry_ts", expiryTs);
                bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_ENTER_TRAFFIC).build());
                AdminSessionDao.save(chatId, State.WAITING_TRAFFIC_GB.name(), data);
            }
            case WAITING_TRAFFIC_GB -> {
                int trafficGb;
                try { trafficGb = Integer.parseInt(text); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_NEED_INT).build());
                    return true;
                }
                AdminSessionDao.delete(chatId);

                XuiClient.AddResult result = XuiClient.get().addClient(
                        (int) data.get("inbound_id"),
                        (String) data.get("email"),
                        (String) data.get("remark"),
                        (long) data.get("expiry_ts"),
                        trafficGb
                );
                if (!result.success()) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.ADD_KEY_ERROR).build());
                    return true;
                }
                long trafficBytes = trafficGb > 0 ? (long) trafficGb * 1024 * 1024 * 1024 : 0;

                Key key = new Key();
                key.userId = (long) data.get("user_db_id");
                key.inboundId = (int) data.get("inbound_id");
                key.xuiClientId = result.clientId();
                key.xuiEmail = (String) data.get("email");
                key.remark = (String) data.get("remark");
                key.expiryTs = (long) data.get("expiry_ts");
                key.trafficTotal = trafficBytes;
                KeyDao.insert(key);

                bot.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text(String.format(Messages.ADD_KEY_SUCCESS, data.get("email"), result.clientId()))
                        .parseMode("HTML").build());
            }
        }
        return true;
    }
}
