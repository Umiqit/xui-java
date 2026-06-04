package bot.handler;

import bot.config.Settings;
import bot.keyboard.Menus;
import bot.middleware.RegisterMiddleware;
import bot.service.XuiApiException;
import bot.util.Messages;
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
                    case "/start"              -> StartHandler.handle(bot, msg);
                    case "Магазин 🏪"          -> ShopHandler.handleShop(bot, msg);
                    case "Кабинет 🪪"          -> CabinetHandler.handleCabinet(bot, msg);
                    case "FAQ ⁉️"              -> CabinetHandler.handleFaq(bot, msg);
                    case "Гарантии ☑️"         -> CabinetHandler.handleWarranty(bot, msg);
                    case "Отзывы 🗣️"           -> CabinetHandler.handleReviews(bot, msg);
                    case "Поддержка 🙋"        -> CabinetHandler.handleSupport(bot, msg);
                    case "Пополнить баланс 💰" -> PaymentHandler.handleTopup(bot, msg);
                    case "История покупок 🔍"  -> CabinetHandler.handleHistory(bot, msg);
                    case "🔑 Мои ключи"        -> KeysHandler.handleList(bot, msg);
                    case "Назад"               -> StartHandler.handle(bot, msg);
                    case "⚙️ Админка"          -> { if (isAdmin) AdminHandler.handlePanel(bot, msg); }
                    case "/xui_inbounds"       -> { if (isAdmin) AdminHandler.handleXuiInbounds(bot, msg); }
                    case "/add_key"            -> { if (isAdmin) AdminHandler.handleAddKeyStart(bot, msg); }
                    case "/users"              -> { if (isAdmin) AdminHandler.handleAddKeyListUsers(bot, msg); }
                    case "/cancel"             -> { if (isAdmin) AdminHandler.cancel(bot, msg); }
                    default -> {
                        if (text.startsWith("/")) {
                            SendMessage sm = SendMessage.builder()
                                    .chatId(msg.getChatId())
                                    .text(Messages.UNKNOWN_COMMAND)
                                    .replyMarkup(isAdmin ? Menus.adminMenu() : Menus.mainMenu())
                                    .build();
                            bot.execute(sm);
                        }
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
                else if (data.startsWith("shop_buy:"))               ShopHandler.handleBuy(bot, call, Long.parseLong(data.split(":")[1]));
                else if (data.startsWith("shop_confirm:"))           ShopHandler.handleConfirm(bot, call, Long.parseLong(data.split(":")[1]));
                else if (data.equals("shop_cancel"))                 {
                    bot.execute(org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText.builder()
                            .chatId(call.getMessage().getChatId().toString())
                            .messageId(call.getMessage().getMessageId())
                            .text("Покупка отменена.")
                            .build());
                }
                else if (data.equals("cabinet_back"))                {
                    CabinetHandler.showCabinet(bot, call.getMessage().getChatId(), call.getFrom().getId());
                }
                else if (data.startsWith("admin_inbound:")) {
                    if (Settings.get().ADMIN_IDS.contains(call.getFrom().getId()))
                        AdminHandler.handleInboundDetail(bot, call, Integer.parseInt(data.split(":")[1]));
                }

            } else if (update.hasPreCheckoutQuery()) {
                PaymentHandler.handlePreCheckout(bot, update.getPreCheckoutQuery());
            }

        } catch (TelegramApiException e) {
            log.error("Telegram API error", e);
        } catch (XuiApiException e) {
            log.error("XUI API error", e);
            try {
                if (update.hasMessage()) {
                    bot.execute(SendMessage.builder()
                            .chatId(update.getMessage().getChatId())
                            .text(Messages.SERVICE_UNAVAILABLE)
                            .build());
                } else if (update.hasCallbackQuery()) {
                    bot.execute(org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery.builder()
                            .callbackQueryId(update.getCallbackQuery().getId())
                            .text(Messages.SERVICE_UNAVAILABLE)
                            .showAlert(true)
                            .build());
                }
            } catch (TelegramApiException ex) {
                log.error("Failed to send error message", ex);
            }
        } catch (Exception e) {
            log.error("Unhandled error in router", e);
        }
    }
}
