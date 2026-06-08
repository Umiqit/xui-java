package bot.service;

import bot.db.dao.ServerDao;
import bot.db.model.Server;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class XuiClientFactory {

    private static final Logger log = LoggerFactory.getLogger(XuiClientFactory.class);
    private static final Map<Long, XuiClient> CACHE = new ConcurrentHashMap<>();

    public static XuiClient get(long serverId) {
        return CACHE.computeIfAbsent(serverId, id -> {
            Server server = ServerDao.findById(id);
            if (server == null) {
                throw new XuiApiException("Server not found: " + id);
            }
            return new XuiClient(server);
        });
    }

    public static void invalidate(long serverId) {
        CACHE.remove(serverId);
    }

    public static void invalidateAll() {
        CACHE.clear();
    }

    /**
     * Picks the best active server based on total client count (least loaded).
     * Returns null if no server is available.
     */
    public static Server pickBestServer() {
        List<Server> servers = ServerDao.findActive();
        if (servers.isEmpty()) {
            log.warn("No active servers configured");
            return null;
        }

        Server best = null;
        int bestClients = Integer.MAX_VALUE;

        for (Server server : servers) {
            try {
                XuiClient client = get(server.id);
                client.login();
                List<JsonNode> inbounds = client.getInbounds();
                int totalClients = 0;
                for (JsonNode inbound : inbounds) {
                    if (inbound.has("clientStats") && inbound.get("clientStats").isArray()) {
                        totalClients += inbound.get("clientStats").size();
                    }
                }
                log.debug("Server {} has {} clients", server.displayName(), totalClients);
                if (totalClients < bestClients) {
                    bestClients = totalClients;
                    best = server;
                }
            } catch (XuiApiException e) {
                log.warn("Server {} is unreachable: {}", server.displayName(), e.getMessage());
                invalidate(server.id);
            }
        }

        return best;
    }

    /**
     * Checks server availability by trying to login and list inbounds.
     */
    public static boolean isAvailable(long serverId) {
        try {
            XuiClient client = get(serverId);
            client.login();
            client.getInbounds();
            return true;
        } catch (XuiApiException e) {
            invalidate(serverId);
            return false;
        }
    }
}
