package bot.handler;

import bot.db.Database;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class ProfileHandler {

    public static void handle(AbsSender bot, Message msg) throws TelegramApiException {
        Connection c = Database.get();
        try {
            long tgId = msg.getFrom().getId();
            long dbId;
            double balance;
            String createdAt;

            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT id, balance, created_at FROM users WHERE tg_id=?")) {
                ps.setLong(1, tgId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return;
                    dbId = rs.getLong("id");
                    balance = rs.getDouble("balance");
                    createdAt = rs.getString("created_at");
                }
            }

            int keysCount;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM keys WHERE user_id=?")) {
                ps.setLong(1, dbId);
                try (ResultSet rs = ps.executeQuery()) {
                    keysCount = rs.next() ? rs.getInt(1) : 0;
                }
            }

            int paymentsCount;
            try (PreparedStatement ps = c.prepareStatement(
                    "SELECT COUNT(*) FROM payments WHERE user_id=? AND status='completed'")) {
                ps.setLong(1, dbId);
                try (ResultSet rs = ps.executeQuery()) {
                    paymentsCount = rs.next() ? rs.getInt(1) : 0;
                }
            }

            String username = msg.getFrom().getUserName();
            String text = "👤 <b>Профиль</b>\n\n" +
                    "ID: <code>" + tgId + "</code>\n" +
                    "Имя: " + msg.getFrom().getFirstName() + "\n" +
                    "Username: @" + (username != null ? username : "—") + "\n\n" +
                    "💰 Баланс: <b>" + String.format("%.2f", balance) + " ⭐</b>\n" +
                    "🔑 Ключей: <b>" + keysCount + "</b>\n" +
                    "💳 Платежей: <b>" + paymentsCount + "</b>\n\n" +
                    "📅 Регистрация: " + createdAt.substring(0, 10);

            bot.execute(SendMessage.builder()
                    .chatId(msg.getChatId())
                    .text(text)
                    .parseMode("HTML")
                    .build());
        } catch (SQLException e) {
            throw new RuntimeException("ProfileHandler failed", e);
        }
    }
}
