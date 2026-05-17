package bot.service;

import bot.db.dao.UserDao;
import bot.db.model.User;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public class AdminService {

    public static String formatUsersList() {
        List<User> users = UserDao.findAll(30);
        if (users.isEmpty()) return null;
        StringBuilder sb = new StringBuilder("<b>Пользователи</b>:\n\n");
        for (User u : users) {
            String name = u.fullName != null ? u.fullName : u.username;
            if (name == null) name = String.valueOf(u.tgId);
            sb.append("• <code>").append(u.tgId).append("</code> ")
              .append(name).append(" — ").append((int) u.balance).append("⭐\n");
        }
        return sb.toString();
    }

    public static String formatInboundDetail(JsonNode inbound) {
        List<JsonNode> clients = new java.util.ArrayList<>();
        inbound.path("clientStats").forEach(clients::add);
        StringBuilder sb = new StringBuilder("📡 <b>" +
                inbound.path("remark").asText(String.valueOf(inbound.path("id").asInt())) + "</b> — " +
                clients.size() + " клиентов\n\n");
        int limit = Math.min(30, clients.size());
        for (int i = 0; i < limit; i++) {
            JsonNode cl = clients.get(i);
            double used = Math.round((cl.path("up").asLong(0) + cl.path("down").asLong(0))
                    / (1024.0 * 1024 * 1024) * 100.0) / 100.0;
            double total = Math.round(cl.path("total").asLong(0) / (1024.0 * 1024 * 1024) * 100.0) / 100.0;
            String en = cl.path("enable").asBoolean(false) ? "🟢" : "🔴";
            String totalStr = total == 0 ? "∞" : String.valueOf(total);
            sb.append(en).append(" <code>").append(cl.path("email").asText())
              .append("</code> — ").append(used).append("/").append(totalStr).append(" GB\n");
        }
        return sb.toString();
    }
}
