package bot;

import bot.config.Settings;
import bot.db.Database;
import bot.handler.UpdateRouter;
import bot.service.XuiApiException;
import bot.service.XuiClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

public class Main extends TelegramLongPollingBot {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public Main() {
        super(Settings.get().BOT_TOKEN);
    }

    @Override
    public String getBotUsername() {
        return Settings.get().BOT_USERNAME;
    }

    @Override
    public void onUpdateReceived(Update update) {
        UpdateRouter.route(this, update);
    }

    public static void main(String[] args) throws TelegramApiException {
        Database.init();
        if (Settings.get().isXuiEnabled()) {
            try {
                XuiClient.get().login();
                log.info("XUI login: OK");
            } catch (XuiApiException e) {
                log.error("XUI login failed: {}", e.getMessage());
            }
        } else {
            log.info("XUI panel not configured, skipping login");
        }

        TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
        api.registerBot(new Main());
        log.info("Bot started");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Database.close();
            log.info("Shutdown complete");
        }));
    }
}
