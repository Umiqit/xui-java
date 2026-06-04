package bot.handler;

import bot.db.dao.KeyDao;
import bot.db.dao.UserDao;
import bot.db.model.Key;
import bot.db.model.User;
import bot.keyboard.Menus;
import bot.service.XuiClient;
import bot.service.XuiApiException;
import bot.util.Messages;
import com.fasterxml.jackson.databind.JsonNode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.UUID;

public class BuyKeyHandler {

    public static final int PLAN_1_PRICE = 100;
    public static final int PLAN_1_DAYS = 30;
    public static final int PLAN_1_GB = 100;

    public static final int PLAN_2_PRICE = 250;
    public static final int PLAN_2_DAYS = 90;
    public static final int PLAN_2_GB = 500;

    public static void handleBuyKeyStart(AbsSender bot, Message msg) throws TelegramApiException {
        try {
            XuiClient.get().login();
            List<JsonNode> inbounds = XuiClient.get().getInbounds();
            if (inbounds.isEmpty()) {
                bot.execute(SendMessage.builder()
                        .chatId(msg.getChatId()).text(Messages.BUY_KEY_NO_INBOUNDS).build());
                return;
            }
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text(Messages.BUY_KEY_TITLE)
                    .replyMarkup(Menus.buyInboundsKeyboard(inbounds))
                    .parseMode("HTML")
                    .build());
        } catch (XuiApiException e) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId()).text(Messages.SERVICE_UNAVAILABLE).build());
        }
    }

    public static void handleInboundSelect(AbsSender bot, CallbackQuery call, int inboundId) throws TelegramApiException {
        String remark = null;
        try {
            XuiClient.get().login();
            List<JsonNode> inbounds = XuiClient.get().getInbounds();
            for (JsonNode ib : inbounds) {
                if (ib.path("id").asInt() == inboundId) {
                    remark = ib.path("remark").asText("Inbound " + inboundId);
                    break;
                }
            }
        } catch (XuiApiException e) {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId()).text(Messages.SERVICE_UNAVAILABLE).showAlert(true).build());
            return;
        }
        if (remark == null) remark = "Inbound " + inboundId;

        bot.execute(EditMessageText.builder()
                .chatId(call.getMessage().getChatId().toString())
                .messageId(call.getMessage().getMessageId())
                .text(String.format(Messages.BUY_KEY_SELECT_PLAN, remark))
                .parseMode("HTML")
                .replyMarkup(Menus.buyPlansKeyboard(inboundId))
                .build());
    }

    public static void handlePlanSelect(AbsSender bot, CallbackQuery call, int inboundId, int planId) throws TelegramApiException {
        long tgId = call.getFrom().getId();
        User user = UserDao.findByTgId(tgId);
        if (user == null) return;

        int price, days, gb;
        if (planId == 1) {
            price = PLAN_1_PRICE; days = PLAN_1_DAYS; gb = PLAN_1_GB;
        } else if (planId == 2) {
            price = PLAN_2_PRICE; days = PLAN_2_DAYS; gb = PLAN_2_GB;
        } else {
            return;
        }

        if (user.balance < price) {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId())
                    .text(String.format("Недостаточно средств. Нужно %d ⭐", price))
                    .showAlert(true)
                    .build());
            return;
        }

        // Deduct balance
        boolean deducted = UserDao.deductBalance(user.id, price);
        if (!deducted) {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId())
                    .text("Ошибка при списании баланса. Попробуй позже.")
                    .showAlert(true)
                    .build());
            return;
        }

        long expiryTs = days > 0 ? (System.currentTimeMillis() + (long) days * 86400 * 1000) : 0;
        String email = "buy_" + tgId + "_" + System.currentTimeMillis();
        String remark = "auto " + tgId;

        try {
            XuiClient.AddResult result = XuiClient.get().addClient(inboundId, email, remark, expiryTs, gb);
            if (!result.success()) {
                // Refund
                UserDao.addBalance(user.id, price);
                bot.execute(EditMessageText.builder()
                        .chatId(call.getMessage().getChatId().toString())
                        .messageId(call.getMessage().getMessageId())
                        .text(Messages.BUY_KEY_ERROR)
                        .build());
                return;
            }

            long trafficBytes = gb > 0 ? (long) gb * 1024 * 1024 * 1024 : 0;
            Key key = new Key();
            key.userId = user.id;
            key.inboundId = inboundId;
            key.xuiClientId = result.clientId();
            key.xuiEmail = email;
            key.remark = remark;
            key.expiryTs = expiryTs;
            key.trafficTotal = trafficBytes;
            KeyDao.insert(key);

            User updatedUser = UserDao.findByTgId(tgId);
            double remaining = updatedUser != null ? updatedUser.balance : 0;

            String expiryStr = days > 0 ? days + " дней" : "Бессрочно";
            String trafficStr = gb > 0 ? gb + " GB" : "Безлимит";
            String inboundName = "Inbound " + inboundId;
            try {
                List<JsonNode> inbounds = XuiClient.get().getInbounds();
                for (JsonNode ib : inbounds) {
                    if (ib.path("id").asInt() == inboundId) {
                        inboundName = ib.path("remark").asText(inboundName);
                        break;
                    }
                }
            } catch (XuiApiException ignored) {}

            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(String.format(Messages.BUY_KEY_SUCCESS, email, result.clientId(), inboundName, expiryStr, trafficStr, price, remaining))
                    .parseMode("HTML")
                    .build());

        } catch (XuiApiException e) {
            // Refund
            UserDao.addBalance(user.id, price);
            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(Messages.BUY_KEY_ERROR)
                    .build());
        }
    }
}
