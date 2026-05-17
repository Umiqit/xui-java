package bot.handler;

import bot.db.dao.UserDao;
import bot.service.PaymentService;
import bot.util.Messages;
import bot.keyboard.Menus;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

public class PaymentHandler {

    private static final int[] AMOUNTS = {50, 100, 250, 500, 1000};

    public static void handleTopup(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(Messages.TOPUP_TITLE)
                .replyMarkup(Menus.paymentKeyboard(AMOUNTS))
                .build());
    }

    public static void handlePayStars(AbsSender bot, CallbackQuery call, int amount) throws TelegramApiException {
        Long userId = UserDao.getIdByTgId(call.getFrom().getId());
        if (userId == null) return;
        long paymentId = PaymentService.createPayment(userId, amount, "XTR",
                "topup_" + userId + "_" + amount);
        bot.execute(SendInvoice.builder()
                .chatId(call.getMessage().getChatId().toString())
                .title(Messages.INVOICE_TITLE)
                .description(String.format(Messages.INVOICE_DESC, amount))
                .payload(String.valueOf(paymentId))
                .currency("XTR")
                .price(new LabeledPrice(amount + " Stars", amount))
                .build());
    }

    public static void handlePreCheckout(AbsSender bot, PreCheckoutQuery query) throws TelegramApiException {
        bot.execute(AnswerPreCheckoutQuery.builder()
                .preCheckoutQueryId(query.getId()).ok(true).build());
    }

    public static void handleSuccessfulPayment(AbsSender bot, Message msg) throws TelegramApiException {
        var payment = msg.getSuccessfulPayment();
        long paymentId = Long.parseLong(payment.getInvoicePayload());
        int amount = payment.getTotalAmount();

        Long userId = UserDao.getIdByTgId(msg.getFrom().getId());
        if (userId == null) return;
        PaymentService.confirmPayment(paymentId, userId, amount);

        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text(String.format(Messages.PAYMENT_SUCCESS, amount))
                .parseMode("HTML")
                .build());
    }
}
