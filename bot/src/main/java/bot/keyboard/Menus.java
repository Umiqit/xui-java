package bot.keyboard;

import bot.db.model.Key;
import bot.db.model.Product;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public class Menus {

    public static ReplyKeyboardMarkup mainMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("Магазин 🏪"));
        r1.add(new KeyboardButton("Кабинет 🪪"));
        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("FAQ ⁉️"));
        r2.add(new KeyboardButton("Гарантии ☑️"));
        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("Отзывы 🗣️"));
        r3.add(new KeyboardButton("Поддержка 🙋"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup(List.of(r1, r2, r3));
        kb.setResizeKeyboard(true);
        return kb;
    }

    public static ReplyKeyboardMarkup adminMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("Магазин 🏪"));
        r1.add(new KeyboardButton("Кабинет 🪪"));
        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("FAQ ⁉️"));
        r2.add(new KeyboardButton("Гарантии ☑️"));
        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("Отзывы 🗣️"));
        r3.add(new KeyboardButton("Поддержка 🙋"));
        KeyboardRow r4 = new KeyboardRow();
        r4.add(new KeyboardButton("⚙️ Админка"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup(List.of(r1, r2, r3, r4));
        kb.setResizeKeyboard(true);
        return kb;
    }

    public static ReplyKeyboardMarkup cabinetMenu() {
        KeyboardRow r1 = new KeyboardRow();
        r1.add(new KeyboardButton("Пополнить баланс 💰"));
        KeyboardRow r2 = new KeyboardRow();
        r2.add(new KeyboardButton("История покупок 🔍"));
        KeyboardRow r3 = new KeyboardRow();
        r3.add(new KeyboardButton("🔑 Мои ключи"));
        KeyboardRow r4 = new KeyboardRow();
        r4.add(new KeyboardButton("Назад"));
        ReplyKeyboardMarkup kb = new ReplyKeyboardMarkup(List.of(r1, r2, r3, r4));
        kb.setResizeKeyboard(true);
        return kb;
    }

    public static InlineKeyboardMarkup keysKeyboard(List<Key> keys) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Key key : keys) {
            boolean expired = key.isExpired();
            String label = expired ? "🔴 " : "🟢 ";
            String name = key.remark;
            if (name == null || name.isBlank()) {
                name = key.xuiEmail;
            }
            label += name;
            InlineKeyboardButton btn = InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData("key_detail:" + key.id)
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
        for (int amount : amounts) {
            row.add(InlineKeyboardButton.builder()
                    .text("⭐ " + amount)
                    .callbackData("pay_stars:" + amount).build());
            if (row.size() == 3) {
                rows.add(row);
                row = new ArrayList<>();
            }
        }
        if (!row.isEmpty()) {
            rows.add(row);
        }
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup shopKeyboard(List<Product> products) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (Product p : products) {
            String traffic = p.trafficGb == 0 ? "∞" : p.trafficGb + " GB";
            String label = String.format("%s — %.0f ⭐ (%d дн, %s)", p.name, p.price, p.durationDays, traffic);
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData("shop_buy:" + p.id)
                    .build()));
        }
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup shopConfirmKeyboard(long productId) {
        return new InlineKeyboardMarkup(List.of(
                List.of(
                        InlineKeyboardButton.builder()
                                .text("✅ Купить")
                                .callbackData("shop_confirm:" + productId)
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("❌ Отмена")
                                .callbackData("shop_cancel")
                                .build()
                )
        ));
    }

    public static InlineKeyboardMarkup backToCabinetKeyboard() {
        return new InlineKeyboardMarkup(List.of(
                List.of(InlineKeyboardButton.builder()
                        .text("◀️ Назад в кабинет")
                        .callbackData("cabinet_back")
                        .build())
        ));
    }

    public static InlineKeyboardMarkup adminInboundsKeyboard(List<com.fasterxml.jackson.databind.JsonNode> inbounds, long serverId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode ib : inbounds) {
            int id = ib.path("id").asInt();
            String remark = ib.path("remark").asText(String.valueOf(id));
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("📡 " + remark + " (id: " + id + ")")
                    .callbackData("admin_inbound:" + serverId + ":" + id).build()));
        }
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup serversKeyboard(List<bot.db.model.Server> servers, String prefix) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (bot.db.model.Server s : servers) {
            String label = s.active ? "🟢 " : "🔴 ";
            label += s.displayName();
            if (s.location != null && !s.location.isBlank()) {
                label += " (" + s.location + ")";
            }
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text(label)
                    .callbackData(prefix + ":" + s.id)
                    .build()));
        }
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup buyInboundsKeyboard(List<com.fasterxml.jackson.databind.JsonNode> inbounds, long serverId) {
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        for (com.fasterxml.jackson.databind.JsonNode ib : inbounds) {
            int id = ib.path("id").asInt();
            String remark = ib.path("remark").asText(String.valueOf(id));
            rows.add(List.of(InlineKeyboardButton.builder()
                    .text("📡 " + remark)
                    .callbackData("buy_inbound:" + serverId + ":" + id).build()));
        }
        return new InlineKeyboardMarkup(rows);
    }

    public static InlineKeyboardMarkup buyPlansKeyboard(long serverId, int inboundId) {
        return new InlineKeyboardMarkup(List.of(
                List.of(InlineKeyboardButton.builder()
                        .text(String.format(bot.util.Messages.BUY_KEY_PLAN_1, bot.handler.BuyKeyHandler.PLAN_1_PRICE))
                        .callbackData("buy_plan:" + serverId + ":" + inboundId + ":1").build()),
                List.of(InlineKeyboardButton.builder()
                        .text(String.format(bot.util.Messages.BUY_KEY_PLAN_2, bot.handler.BuyKeyHandler.PLAN_2_PRICE))
                        .callbackData("buy_plan:" + serverId + ":" + inboundId + ":2").build())
        ));
    }
}
