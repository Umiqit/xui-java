package bot.handler;

import bot.config.Settings;
import bot.keyboard.Menus;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class StartHandler {

    public static void handle(AbsSender bot, Message msg) throws TelegramApiException {
        long uid = msg.getFrom().getId();
        boolean isAdmin = Settings.get().ADMIN_IDS.contains(uid);
        String name = msg.getFrom().getFirstName();
        SendMessage sm = SendMessage.builder()
                .chatId(msg.getChatId())
                .text("Привет, <b>" + name + "</b>!\n\nУправляй своими VPN-ключами через бота.")
                .parseMode("HTML")
                .replyMarkup(isAdmin ? Menus.adminMenu() : Menus.mainMenu())
                .build();
        bot.execute(sm);
    }
}
