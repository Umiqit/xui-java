package bot.handler;

import bot.db.dao.UserDao;
import bot.db.model.User;
import bot.util.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class BalanceHandler {

    public static void handle(AbsSender bot, Message msg) throws TelegramApiException {
        long tgId = msg.getFrom().getId();
        User user = UserDao.findByTgId(tgId);
        if (user == null) return;

        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(String.format(Messages.BALANCE_TEXT, user.balance))
                .parseMode("HTML")
                .build());
    }
}
