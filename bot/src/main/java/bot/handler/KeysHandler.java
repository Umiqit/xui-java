package bot.handler;

import bot.db.model.Key;
import bot.db.model.Server;
import bot.db.dao.ServerDao;
import bot.keyboard.Menus;
import bot.service.KeyService;
import bot.util.Messages;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageReplyMarkup;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class KeysHandler {

    public static void handleList(AbsSender bot, Message msg) throws TelegramApiException {
        List<Key> keys = KeyService.getUserKeys(msg.getFrom().getId());
        if (keys.isEmpty()) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId()).text(Messages.NO_KEYS).build());
            return;
        }
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(String.format(Messages.KEYS_TITLE, keys.size()))
                .parseMode("HTML")
                .replyMarkup(Menus.keysKeyboard(keys))
                .build());
    }

    public static void handleKeysList(AbsSender bot, CallbackQuery call) throws TelegramApiException {
        List<Key> keys = KeyService.getUserKeys(call.getFrom().getId());
        String chatId = call.getMessage().getChatId().toString();
        int msgId = call.getMessage().getMessageId();
        if (keys.isEmpty()) {
            bot.execute(EditMessageText.builder()
                    .chatId(chatId).messageId(msgId).text(Messages.NO_KEYS).build());
            return;
        }
        bot.execute(EditMessageText.builder()
                .chatId(chatId).messageId(msgId)
                .text(String.format(Messages.KEYS_TITLE, keys.size()))
                .parseMode("HTML")
                .replyMarkup(Menus.keysKeyboard(keys))
                .build());
    }

    public static void handleRefresh(AbsSender bot, CallbackQuery call) throws TelegramApiException {
        KeyService.refreshAllUserKeys(call.getFrom().getId());
        bot.execute(AnswerCallbackQuery.builder()
                .callbackQueryId(call.getId()).text(Messages.REFRESHED).build());
        List<Key> keys = KeyService.getUserKeys(call.getFrom().getId());
        bot.execute(EditMessageReplyMarkup.builder()
                .chatId(call.getMessage().getChatId().toString())
                .messageId(call.getMessage().getMessageId())
                .replyMarkup(Menus.keysKeyboard(keys))
                .build());
    }

    public static void handleDetail(AbsSender bot, CallbackQuery call, long keyId) throws TelegramApiException {
        Key k = KeyService.getKeyDetail(keyId, call.getFrom().getId());
        if (k == null) {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.KEY_NOT_FOUND).showAlert(true).build());
            return;
        }
        String status = k.isExpired() ? "🔴 Истёк" : "🟢 Активен";
        String totalStr = k.trafficTotalGb() == 0 ? "∞" : String.valueOf(k.trafficTotalGb());
        String serverName = "";
        try {
            Server srv = ServerDao.findById(k.serverId);
            serverName = srv != null ? srv.displayName() : "Unknown";
        } catch (Exception ignored) {}
        String text = "🔑 <b>" + (k.remark != null && !k.remark.isBlank() ? k.remark : k.xuiEmail) + "</b>\n\n" +
                "Статус: " + status + "\n" +
                "🖥 Сервер: " + serverName + "\n" +
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

    public static void handleResetTraffic(AbsSender bot, CallbackQuery call, long keyId) throws TelegramApiException {
        try {
            KeyService.resetTraffic(keyId, call.getFrom().getId());
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.TRAFFIC_RESET_OK).showAlert(true).build());
        } catch (IllegalArgumentException e) {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.KEY_NOT_FOUND).showAlert(true).build());
        }
    }

    public static void handleDelete(AbsSender bot, CallbackQuery call, long keyId) throws TelegramApiException {
        try {
            KeyService.deleteKey(keyId, call.getFrom().getId());
            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(Messages.KEY_DELETED).build());
        } catch (IllegalArgumentException e) {
            bot.execute(AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.KEY_NOT_FOUND).showAlert(true).build());
        }
    }
}
