package bot.handler;

import bot.db.dao.UserDao;
import bot.db.model.User;
import bot.util.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class ProfileHandler {

    public static void handle(AbsSender bot, Message msg) throws TelegramApiException {
        long tgId = msg.getFrom().getId();
        User user = UserDao.findByTgId(tgId);
        if (user == null) return;

        int keysCount = UserDao.countKeys(user.id);
        int paymentsCount = UserDao.countCompletedPayments(user.id);
        String username = msg.getFrom().getUserName();

        String text = "👤 <b>Профиль</b>\n\n" +
                "ID: <code>" + tgId + "</code>\n" +
                "Имя: " + msg.getFrom().getFirstName() + "\n" +
                "Username: @" + (username != null ? username : "—") + "\n\n" +
                "💰 Баланс: <b>" + String.format("%.2f", user.balance) + " ⭐</b>\n" +
                "🔑 Ключей: <b>" + keysCount + "</b>\n" +
                "💳 Платежей: <b>" + paymentsCount + "</b>\n\n" +
                "📅 Регистрация: " + user.createdAt.substring(0, 10);

        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(text)
                .parseMode("HTML")
                .build());
    }
}
