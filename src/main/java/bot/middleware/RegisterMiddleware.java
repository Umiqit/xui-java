package bot.middleware;

import bot.db.Database;
import org.telegram.telegrambots.meta.api.objects.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class RegisterMiddleware {

    public static void register(User tgUser) {
        if (tgUser == null || Boolean.TRUE.equals(tgUser.getIsBot())) return;
        Connection c = Database.get();
        try (PreparedStatement ps = c.prepareStatement("""
                INSERT INTO users (tg_id, username, full_name)
                VALUES (?, ?, ?)
                ON CONFLICT(tg_id) DO UPDATE SET
                    username=excluded.username,
                    full_name=excluded.full_name
                """)) {
            ps.setLong(1, tgUser.getId());
            ps.setString(2, tgUser.getUserName());
            String fullName = tgUser.getFirstName() +
                    (tgUser.getLastName() != null ? " " + tgUser.getLastName() : "");
            ps.setString(3, fullName);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("RegisterMiddleware failed", e);
        }
    }
}
