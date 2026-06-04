package bot.handler;

import bot.config.Settings;
import bot.db.dao.OrderDao;
import bot.db.dao.UserDao;
import bot.db.model.Order;
import bot.db.model.User;
import bot.keyboard.Menus;
import bot.util.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;

public class CabinetHandler {

    public static void handleCabinet(AbsSender bot, Message msg) throws TelegramApiException {
        showCabinet(bot, msg.getChatId(), msg.getFrom().getId());
    }

    public static void showCabinet(AbsSender bot, long chatId, long tgId) throws TelegramApiException {
        User user = UserDao.findByTgId(tgId);
        if (user == null) return;

        int paymentsCount = UserDao.countCompletedPayments(user.id);
        int ordersCount = UserDao.countOrders(user.id);

        String text = String.format(Messages.CABINET_TITLE, paymentsCount, ordersCount, user.balance);

        bot.execute(SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .parseMode("HTML")
                .replyMarkup(Menus.cabinetMenu())
                .build());
    }

    public static void handleHistory(AbsSender bot, Message msg) throws TelegramApiException {
        long tgId = msg.getFrom().getId();
        User user = UserDao.findByTgId(tgId);
        if (user == null) return;

        List<Order> orders = OrderDao.findByUserId(user.id);
        if (orders.isEmpty()) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text(Messages.CABINET_NO_ORDERS)
                    .replyMarkup(Menus.cabinetMenu())
                    .build());
            return;
        }

        StringBuilder sb = new StringBuilder(Messages.CABINET_ORDERS_TITLE);
        for (Order o : orders) {
            String date = o.createdAt != null && o.createdAt.length() >= 10 ? o.createdAt.substring(0, 10) : o.createdAt;
            sb.append("• ").append(o.productName)
              .append(" — ").append(String.format("%.0f", o.amount)).append(" ⭐")
              .append(" (").append(date).append(")\n");
        }

        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(sb.toString())
                .parseMode("HTML")
                .replyMarkup(Menus.cabinetMenu())
                .build());
    }

    public static void handleFaq(AbsSender bot, Message msg) throws TelegramApiException {
        String url = Settings.get().SITE_URL;
        if (url == null || url.isBlank()) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text("Сайт временно недоступен.")
                    .build());
            return;
        }
        String link = url.replaceAll("/+$", "") + "/faq.html";
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.FAQ_TEXT + "\n" + link)
                .replyMarkup(Menus.mainMenu())
                .build());
    }

    public static void handleWarranty(AbsSender bot, Message msg) throws TelegramApiException {
        String url = Settings.get().SITE_URL;
        if (url == null || url.isBlank()) {
            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text("Сайт временно недоступен.")
                    .build());
            return;
        }
        String link = url.replaceAll("/+$", "") + "/warranty.html";
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.WARRANTY_TEXT + "\n" + link)
                .replyMarkup(Menus.mainMenu())
                .build());
    }

    public static void handleReviews(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.REVIEWS_TEXT)
                .replyMarkup(Menus.mainMenu())
                .build());
    }

    public static void handleSupport(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.SUPPORT_TEXT)
                .replyMarkup(Menus.mainMenu())
                .build());
    }
}
