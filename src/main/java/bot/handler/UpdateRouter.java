package bot.handler;

import bot.config.Settings;
import bot.keyboard.Menus;
import bot.middleware.RegisterMiddleware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class UpdateRouter {

    private static final Logger log = LoggerFactory.getLogger(UpdateRouter.class);

    public static void route(AbsSender bot, Update update) {
        try {
            if (update.hasMessage()) {
                Message msg = update.getMessage();
                if (msg.getFrom() != null) RegisterMiddleware.register(msg.getFrom());

                if (msg.hasSuccessfulPayment()) {
                    PaymentHandler.handleSuccessfulPayment(bot, msg);
                    return;
                }

                if (!msg.hasText()) return;
                String text = msg.getText();
                long uid = msg.getFrom().getId();
                boolean isAdmin = Settings.get().ADMIN_IDS.contains(uid);

                // Admin FSM check first
                if (isAdmin && AdminHandler.handleFsmStep(bot, msg)) return;

                switch (text) {
                    case "/start"        -> StartHandler.handle(bot, msg);
                    case "👤 Профиль"    -> ProfileHandler.handle(bot, msg);
                    case "🔑 Мои ключи"  -> KeysHandler.handleList(bot, msg);
                    case "💳 Пополнить"  -> PaymentHandler.handleTopup(bot, msg);
                    case "📊 Статистика" -> ProfileHandler.handle(bot, msg); // same view
                    case "⚙️ Админка"    -> { if (isAdmin) AdminHandler.handlePanel(bot, msg); }
                    case "/xui_inbounds" -> { if (isAdmin) AdminHandler.handleXuiInbounds(bot, msg); }
                    case "/add_key"      -> { if (isAdmin) AdminHandler.handleAddKeyStart(bot, msg); }
                    case "/users"        -> { if (isAdmin) AdminHandler.handleAddKeyListUsers(bot, msg); }
                    default -> {
                        SendMessage sm = SendMessage.builder()
                                .chatId(msg.getChatId())
                                .text("Не понял команду. Используй кнопки меню.")
                                .replyMarkup(isAdmin ? Menus.adminMenu() : Menus.mainMenu())
                                .build();
                        bot.execute(sm);
                    }
                }

            } else if (update.hasCallbackQuery()) {
                CallbackQuery call = update.getCallbackQuery();
                if (call.getFrom() != null) RegisterMiddleware.register(call.getFrom());
                String data = call.getData();

                if (data.equals("keys_list"))                        KeysHandler.handleKeysList(bot, call);
                else if (data.equals("keys_refresh"))                KeysHandler.handleRefresh(bot, call);
                else if (data.startsWith("key_detail:"))             KeysHandler.handleDetail(bot, call, Long.parseLong(data.split(":")[1]));
                else if (data.startsWith("key_reset_traffic:"))      KeysHandler.handleResetTraffic(bot, call, Long.parseLong(data.split(":")[1]));
                else if (data.startsWith("key_delete:"))             KeysHandler.handleDelete(bot, call, Long.parseLong(data.split(":")[1]));
                else if (data.startsWith("pay_stars:"))              PaymentHandler.handlePayStars(bot, call, Integer.parseInt(data.split(":")[1]));
                else if (data.startsWith("admin_inbound:")) {
                    if (Settings.get().ADMIN_IDS.contains(call.getFrom().getId()))
                        AdminHandler.handleInboundDetail(bot, call, Integer.parseInt(data.split(":")[1]));
                }

            } else if (update.hasPreCheckoutQuery()) {
                PaymentHandler.handlePreCheckout(bot, update.getPreCheckoutQuery());
            }

        } catch (TelegramApiException e) {
            log.error("Telegram API error", e);
        } catch (Exception e) {
            log.error("Unhandled error in router", e);
        }
    }
}
