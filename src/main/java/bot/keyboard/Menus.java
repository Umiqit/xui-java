package bot.keyboard;

import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Menus {

    public static ReplyKeyboardMarkup mainMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("👤 Профиль"));
        r1.add(new KeyboardButton("🔑 Мои ключи"));
        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("💳 Пополнить"));
        r2.add(new KeyboardButton("📊 Статистика"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup(List.of(r1, r2));
        kb.setResizeKeyboard(true);
        return kb;
    }

    public static ReplyKeyboardMarkup adminMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("👤 Профиль"));
        r1.add(new KeyboardButton("🔑 Мои ключи"));
        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("💳 Пополнить"));
        r2.add(new KeyboardButton("📊 Статистика"));
        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("⚙️ Админка"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup(List.of(r1, r2, r3));
        kb.setResizeKeyboard(true);
        return kb;
    }

    // keys: List of maps with keys: id, remark, xui_email, is_expired
    public static InlineKeyboardMarkup keysKeyboard(List<Map<String, Object>> keys) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Map<String, Object> key : keys) {
            boolean expired = (boolean) key.get("is_expired");
            String label = expired ? "🔴 " : "🟢 ";
            String name = (String) key.get("remark");
            if (name == null || name.isBlank()) name = (String) key.get("xui_email");
            label += name;
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData("key_detail:" + key.get("id"))
                    .build();
            rows.add(List.of(btn));
        }
        rows.add(List.of(InlineKeyboardButton.builder()
                .text("🔄 Обновить").callbackData("keys_refresh").build()));
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup keyDetailKeyboard(long keyId) {
        return new InlineKeyboardMarkup(List.of(
                List.of(InlineKeyboardButton.builder()
                        .text("🔄 Сбросить трафик")
                        .callbackData("key_reset_traffic:" + keyId).build()),
                List.of(InlineKeyboardButton.builder()
                        .text("❌ Удалить ключ")
                        .callbackData("key_delete:" + keyId).build()),
                List.of(InlineKeyboardButton.builder()
                        .text("◀️ Назад")
                        .callbackData("keys_list").build())
        ));
    }

    public static InlineKeyboardMarkup paymentKeyboard(int[] amounts) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();
        for (int i = 0; i < amounts.length; i++) {
            row.add(InlineKeyboardButton.builder()
                    .text("⭐ " + amounts[i])
                    .callbackData("pay_stars:" + amounts[i]).build());
            if (row.size() == 3) { rows.add(row); row = new ArrayList<>(); }
        }
        if (!row.isEmpty()) rows.add(row);
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup adminInboundsKeyboard(List<com.fasterxml.jackson.databind.JsonNode> inbounds) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode ib : inbounds) {
            int id = ib.path("id").asInt();
            String remark = ib.path("remark").asText(String.valueOf(id));
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("📡 " + remark + " (id: " + id + ")")
                    .callbackData("admin_inbound:" + id).build()));
        }
        return new InlineKeyboardMarkup(rows);
    }
}
