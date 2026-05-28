package bot.handler;

import bot.db.dao.KeyDao;
import bot.db.dao.OrderDao;
import bot.db.dao.ProductDao;
import bot.db.dao.UserDao;
import bot.db.model.Key;
import bot.db.model.Order;
import bot.db.model.Product;
import bot.db.model.User;
import bot.keyboard.Menus;
import bot.service.XuiApiException;
import bot.service.XuiClient;
import bot.util.Messages;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.UUID;

public class ShopHandler {

    private static final Logger log = LoggerFactory.getLogger(ShopHandler.class);

    public static void handleShop(AbsSender bot, Message msg) throws TelegramApiException {
        List<Product> products = ProductDao.findAllActive();
        if (products.isEmpty()) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text(Messages.SHOP_NO_PRODUCTS)
                    .build());
            return;
        }
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.SHOP_TITLE)
                .parseMode("HTML")
                .replyMarkup(Menus.shopKeyboard(products))
                .build());
    }

    public static void handleBuy(AbsSender bot, CallbackQuery call, long productId) throws TelegramApiException {
        Product product = ProductDao.findById(productId);
        if (product == null) {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId())
                    .text("Товар не найден")
                    .showAlert(true)
                    .build());
            return;
        }
        String text = String.format(Messages.SHOP_BUY_CONFIRM, product.name, product.price);
        bot.execute(EditMessageText.builder()
                .chatId(call.getMessage().getChatId().toString())
                .messageId(call.getMessage().getMessageId())
                .text(text)
                .parseMode("HTML")
                .replyMarkup(Menus.shopConfirmKeyboard(productId))
                .build());
    }

    public static void handleConfirm(AbsSender bot, CallbackQuery call, long productId) throws TelegramApiException {
        long tgId = call.getFrom().getId();
        User user = UserDao.findByTgId(tgId);
        if (user == null) {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId())
                    .text("Пользователь не найден")
                    .showAlert(true)
                    .build());
            return;
        }

        Product product = ProductDao.findById(productId);
        if (product == null) {
            bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                    .callbackQueryId(call.getId())
                    .text("Товар не найден")
                    .showAlert(true)
                    .build());
            return;
        }

        if (user.balance < product.price) {
            String text = String.format(Messages.SHOP_BUY_NO_BALANCE, user.balance, product.price);
            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(text)
                    .parseMode("HTML")
                    .replyMarkup(Menus.backToCabinetKeyboard())
                    .build());
            return;
        }

        // Deduct balance
        boolean deducted = UserDao.deductBalance(user.id, product.price);
        if (!deducted) {
            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(Messages.SHOP_BUY_ERROR)
                    .parseMode("HTML")
                    .replyMarkup(Menus.backToCabinetKeyboard())
                    .build());
            return;
        }

        // Create key on XUI panel
        long expiryTs = product.durationDays > 0
                ? System.currentTimeMillis() + (long) product.durationDays * 86400 * 1000
                : 0;
        String email = "dc_" + tgId + "_" + UUID.randomUUID().toString().substring(0, 8);

        try {
            XuiClient.get().login();
            XuiClient.AddResult result = XuiClient.get().addClient(
                    product.inboundId,
                    email,
                    product.name,
                    expiryTs,
                    product.trafficGb
            );
            if (!result.success()) {
                // Refund balance
                UserDao.deductBalance(user.id, -product.price);
                bot.execute(EditMessageText.builder()
                        .chatId(call.getMessage().getChatId().toString())
                        .messageId(call.getMessage().getMessageId())
                        .text(Messages.SHOP_BUY_ERROR)
                        .parseMode("HTML")
                        .replyMarkup(Menus.backToCabinetKeyboard())
                        .build());
                return;
            }

            // Save key to DB
            Key key = new Key();
            key.userId = user.id;
            key.inboundId = product.inboundId;
            key.xuiClientId = result.clientId();
            key.xuiEmail = email;
            key.remark = product.name;
            key.expiryTs = expiryTs;
            key.trafficTotal = product.trafficGb > 0 ? (long) product.trafficGb * 1024 * 1024 * 1024 : 0;
            long keyId = KeyDao.insert(key);

            // Save order
            Order order = new Order();
            order.userId = user.id;
            order.productId = product.id;
            order.keyId = keyId;
            order.amount = product.price;
            order.status = "completed";
            OrderDao.create(order);

            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(Messages.SHOP_BUY_SUCCESS)
                    .parseMode("HTML")
                    .replyMarkup(Menus.backToCabinetKeyboard())
                    .build());

        } catch (XuiApiException e) {
            log.error("Shop purchase XUI error", e);
            // Refund balance
            UserDao.deductBalance(user.id, -product.price);
            bot.execute(EditMessageText.builder()
                    .chatId(call.getMessage().getChatId().toString())
                    .messageId(call.getMessage().getMessageId())
                    .text(Messages.SHOP_BUY_ERROR)
                    .parseMode("HTML")
                    .replyMarkup(Menus.backToCabinetKeyboard())
                    .build());
        }
    }
}
