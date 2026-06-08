package bot.handler;

import bot.keyboard.Menus;
import bot.util.Messages;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class AboutHandler {

    public static void handleAbout(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.ABOUT_TEXT)
                .parseMode("HTML")
                .replyMarkup(Menus.aboutKeyboard())
                .build());
    }
}
