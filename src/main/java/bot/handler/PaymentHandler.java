package bot.handler;

import bot.db.Database;
import bot.keyboard.Menus;
import bot.service.PaymentService;
import org.telegram.telegrambots.meta.api.methods.AnswerPreCheckoutQuery;
import org.telegram.telegrambots.meta.api.methods.invoices.SendInvoice;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.payments.LabeledPrice;
import org.telegram.telegrambots.meta.api.objects.payments.PreCheckoutQuery;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class PaymentHandler {

    private static final int[] AMOUNTS = {50, 100, 250, 500, 1000};

    public static void handleTopup(AbsSender bot, Message msg) throws TelegramApiException {
        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text("Выбери сумму пополнения (в Telegram Stars ⭐):")
                .replyMarkup(Menus.paymentKeyboard(AMOUNTS))
                .build());
    }

    public static void handlePayStars(AbsSender bot, CallbackQuery call, int amount) throws TelegramApiException {
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE tg_id=?")) {
            ps.setLong(1, call.getFrom().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                long userId = rs.getLong("id");
                long paymentId = PaymentService.createPayment(userId, amount, "XTR",
                        "topup_" + userId + "_" + amount);
                bot.execute(SendInvoice.builder()
                        .chatId(call.getMessage().getChatId().toString())
                        .title("Пополнение баланса")
                        .description("Пополнение на " + amount + " ⭐")
                        .payload(String.valueOf(paymentId))
                        .currency("XTR")
                        .price(new LabeledPrice(amount + " Stars", amount))
                        .build());
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public static void handlePreCheckout(AbsSender bot, PreCheckoutQuery query) throws TelegramApiException {
        bot.execute(AnswerPreCheckoutQuery.builder()
                .preCheckoutQueryId(query.getId()).ok(true).build());
    }

    public static void handleSuccessfulPayment(AbsSender bot, Message msg) throws TelegramApiException {
        var payment = msg.getSuccessfulPayment();
        long paymentId = Long.parseLong(payment.getInvoicePayload());
        int amount = payment.getTotalAmount();

        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement("SELECT id FROM users WHERE tg_id=?")) {
            ps.setLong(1, msg.getFrom().getId());
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return;
                long userId = rs.getLong("id");
                PaymentService.confirmPayment(paymentId, userId, amount);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        bot.execute(SendMessage.builder()
                .chatId(msg.getChatId())
                .text("✅ Оплата прошла! Баланс пополнен на <b>" + amount + " ⭐</b>")
                .parseMode("HTML")
                .build());
    }
}
