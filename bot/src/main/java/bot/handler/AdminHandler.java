package bot.handler;

import bot.db.dao.AdminSessionDao;
import bot.db.dao.KeyDao;
import bot.db.dao.ServerDao;
import bot.db.dao.UserDao;
import bot.db.model.AdminSession;
import bot.db.model.Key;
import bot.db.model.Server;
import bot.keyboard.Menus;
import bot.service.AdminService;
import bot.service.XuiClient;
import bot.service.XuiClientFactory;
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
        WAITING_SERVER,
        WAITING_INBOUND,
        WAITING_EMAIL,
        WAITING_REMARK,
        WAITING_EXPIRY_DAYS,
        WAITING_TRAFFIC_GB,
        // Server management
        WAITING_SERVER_NAME,
        WAITING_SERVER_LOCATION,
        WAITING_SERVER_URL,
        WAITING_SERVER_USERNAME,
        WAITING_SERVER_PASSWORD,
        WAITING_SERVER_CERT,
        WAITING_DEL_SERVER_ID
    }

    public static void handlePanel(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.ADMIN_PANEL)
                .parseMode("HTML")
                .build());
    }

    public static void handleXuiInbounds(AbsSender bot, Message msg) throws TelegramApiException {
        List<Server> servers = ServerDao.findAll();
        if (servers.isEmpty()) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId()).text(Messages.NO_SERVERS).build());
            return;
        }
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text("Выбери сервер:")
                .replyMarkup(Menus.serversKeyboard(servers, "admin_server"))
                .build());
    }

    public static void handleServerInbounds(AbsSender bot, CallbackQuery call, long serverId) throws TelegramApiException {
        try {
            XuiClient client = XuiClientFactory.get(serverId);
            client.login();
            List<JsonNode> inbounds = client.getInbounds();
            if (inbounds.isEmpty()) {
                bot.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(call.getId()).text(Messages.XUI_NO_INBOUNDS).showAlert(true).build());
                return;
            }
            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text("Выбери inbound:")
                    .replyMarkup(Menus.adminInboundsKeyboard(inbounds, serverId))
                    .build());
        } catch (XuiApiException e) {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.SERVICE_UNAVAILABLE).showAlert(true).build());
        }
    }

    public static void handleInboundDetail(AbsSender bot, CallbackQuery call, long serverId, int inboundId) throws TelegramApiException {
        try {
            XuiClient client = XuiClientFactory.get(serverId);
            client.login();
            List<JsonNode> inbounds = client.getInbounds();
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

    public static void handleServers(AbsSender bot, Message msg) throws TelegramApiException {
        List<Server> servers = ServerDao.findAll();
        if (servers.isEmpty()) {
            bot.execute(SendMessage.builder().chatId(msg.getChatId()).text(Messages.NO_SERVERS).build());
            return;
        }
        StringBuilder sb = new StringBuilder("🖥 <b>Серверы</b>:\n\n");
        for (Server s : servers) {
            String status = s.active ? "🟢" : "🔴";
            boolean avail = XuiClientFactory.isAvailable(s.id);
            String conn = avail ? "✅" : "❌";
            sb.append(status).append(" ").append(conn).append(" <b>").append(s.displayName()).append("</b>\n")
              .append("   ID: ").append(s.id)
              .append(" | Loc: ").append(s.displayLocation())
              .append(" | ").append(s.url).append("\n\n");
        }
        sb.append("<b>Команды:</b>\n/add_server — добавить сервер\n/del_server — удалить сервер");
        bot.execute(SendMessage.builder().chatId(msg.getChatId()).text(sb.toString()).parseMode("HTML").build());
    }

    public static void handleAddServerStart(AbsSender bot, Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        AdminSessionDao.save(chatId, State.WAITING_SERVER_NAME.name(), new HashMap<>());
        bot.execute(SendMessage.builder().chatId(chatId).text("Название сервера:").build());
    }

    public static void handleDelServerStart(AbsSender bot, Message msg) throws TelegramApiException {
        long chatId = msg.getChatId();
        List<Server> servers = ServerDao.findAll();
        if (servers.isEmpty()) {
            bot.execute(SendMessage.builder().chatId(chatId).text(Messages.NO_SERVERS).build());
            return;
        }
        AdminSessionDao.save(chatId, State.WAITING_DEL_SERVER_ID.name(), new HashMap<>());
        StringBuilder sb = new StringBuilder("Выбери ID сервера для удаления:\n\n");
        for (Server s : servers) {
            sb.append(s.id).append(" — ").append(s.displayName()).append(" (").append(s.url).append(")\n");
        }
        bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString()).build());
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

                List<Server> servers = ServerDao.findActive();
                if (servers.isEmpty()) {
                    bot.execute(SendMessage.builder().chatId(chatId).text(Messages.BUY_KEY_NO_SERVERS).build());
                    AdminSessionDao.delete(chatId);
                    return true;
                }
                bot.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("Выбери сервер:")
                        .replyMarkup(Menus.serversKeyboard(servers, "admin_addkey_server"))
                        .build());
                AdminSessionDao.save(chatId, State.WAITING_SERVER.name(), data);
            }
            case WAITING_SERVER -> {
                // handled via callback, should not reach here
                AdminSessionDao.delete(chatId);
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

                long serverId = ((Number) data.get("server_id")).longValue();
                int inboundId = (int) data.get("inbound_id");

                XuiClient client = XuiClientFactory.get(serverId);
                XuiClient.AddResult result = client.addClient(
                        inboundId,
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
                key.serverId = serverId;
                key.inboundId = inboundId;
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
            // Server management FSM
            case WAITING_SERVER_NAME -> {
                data.put("name", text);
                bot.execute(SendMessage.builder().chatId(chatId).text("Локация (страна/город):").build());
                AdminSessionDao.save(chatId, State.WAITING_SERVER_LOCATION.name(), data);
            }
            case WAITING_SERVER_LOCATION -> {
                data.put("location", text);
                bot.execute(SendMessage.builder().chatId(chatId).text("URL панели x-ui:").build());
                AdminSessionDao.save(chatId, State.WAITING_SERVER_URL.name(), data);
            }
            case WAITING_SERVER_URL -> {
                data.put("url", text);
                bot.execute(SendMessage.builder().chatId(chatId).text("Username для панели:").build());
                AdminSessionDao.save(chatId, State.WAITING_SERVER_USERNAME.name(), data);
            }
            case WAITING_SERVER_USERNAME -> {
                data.put("username", text);
                bot.execute(SendMessage.builder().chatId(chatId).text("Password для панели:").build());
                AdminSessionDao.save(chatId, State.WAITING_SERVER_PASSWORD.name(), data);
            }
            case WAITING_SERVER_PASSWORD -> {
                data.put("password", text);
                bot.execute(SendMessage.builder().chatId(chatId).text("Путь к сертификату (если self-signed, иначе пусто):").build());
                AdminSessionDao.save(chatId, State.WAITING_SERVER_CERT.name(), data);
            }
            case WAITING_SERVER_CERT -> {
                String certPath = text.isBlank() ? "" : text;
                AdminSessionDao.delete(chatId);

                Server s = new Server();
                s.name = (String) data.get("name");
                s.location = (String) data.get("location");
                s.url = (String) data.get("url");
                s.username = (String) data.get("username");
                s.password = (String) data.get("password");
                s.certPath = certPath;
                s.active = true;
                s.weight = 1;
                long id = ServerDao.insert(s);

                bot.execute(SendMessage.builder()
                        .chatId(chatId)
                        .text("✅ Сервер добавлен! ID: " + id)
                        .build());
            }
            case WAITING_DEL_SERVER_ID -> {
                long delId;
                try { delId = Long.parseLong(text); }
                catch (NumberFormatException e) {
                    bot.execute(SendMessage.builder().chatId(chatId).text("Нужен числовой ID.").build());
                    return true;
                }
                AdminSessionDao.delete(chatId);
                ServerDao.delete(delId);
                bot.execute(SendMessage.builder().chatId(chatId).text("🗑 Сервер удалён.").build());
            }
        }
        return true;
    }

    public static void handleAddKeyServerSelect(AbsSender bot, CallbackQuery call, long serverId) throws TelegramApiException {
        long chatId = call.getMessage().getChatId();
        AdminSession session = AdminSessionDao.findByAdminTgId(chatId);
        if (session == null) return;
        Map<String, Object> data = session.data != null ? session.data : new HashMap<>();
        data.put("server_id", serverId);

        try {
            XuiClient client = XuiClientFactory.get(serverId);
            client.login();
            List<JsonNode> inbounds = client.getInbounds();
            if (inbounds.isEmpty()) {
                bot.execute(AnswerCallbackQuery.builder()
                        .callbackQueryId(call.getId()).text(Messages.ADD_KEY_NO_INBOUNDS).showAlert(true).build());
                AdminSessionDao.delete(chatId);
                return;
            }
            StringBuilder sb = new StringBuilder("Inbound'ы:\n");
            for (JsonNode ib : inbounds)
                sb.append("  ").append(ib.path("id").asInt()).append(" — ").append(ib.path("remark").asText("")).append("\n");
            sb.append(Messages.ADD_KEY_ENTER_INBOUND);
            bot.execute(SendMessage.builder().chatId(chatId).text(sb.toString()).build());
            AdminSessionDao.save(chatId, State.WAITING_INBOUND.name(), data);
        } catch (XuiApiException e) {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.SERVICE_UNAVAILABLE).showAlert(true).build());
            AdminSessionDao.delete(chatId);
        }
    }
}
