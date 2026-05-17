package bot.service;

import bot.config.Settings;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.*;
import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class XuiClient {

    private static final Logger log = LoggerFactory.getLogger(XuiClient.class);
    private static final XuiClient INSTANCE = new XuiClient();
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");

    private final OkHttpClient http;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String baseUrl;

    private XuiClient() {
        baseUrl = Settings.get().XUI_URL.replaceAll("/+$", "");
        http = buildClient();
    }

    public static XuiClient get() { return INSTANCE; }

    private OkHttpClient buildClient() {
        try {
            TrustManager[] tm = {new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] c, String a) {}
                public void checkServerTrusted(X509Certificate[] c, String a) {}
                public X509Certificate[] getAcceptedIssuers() { return new X509Certificate[0]; }
            }};
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, tm, new java.security.SecureRandom());
            return new OkHttpClient.Builder()
                    .sslSocketFactory(sc.getSocketFactory(), (X509TrustManager) tm[0])
                    .hostnameVerifier((h, s) -> true)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(15, TimeUnit.SECONDS)
                    .cookieJar(new CookieJar() {
                        private final List<Cookie> cookies = new ArrayList<>();
                        public void saveFromResponse(HttpUrl url, List<Cookie> c) { cookies.addAll(c); }
                        public List<Cookie> loadForRequest(HttpUrl url) { return cookies; }
                    })
                    .build();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean login() {
        String body = "username=" + Settings.get().XUI_USERNAME +
                      "&password=" + Settings.get().XUI_PASSWORD;
        Request req = new Request.Builder()
                .url(baseUrl + "/login")
                .post(RequestBody.create(body, FORM))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            JsonNode node = mapper.readTree(resp.body().string());
            return node.path("success").asBoolean(false);
        } catch (IOException e) {
            log.error("XUI login failed", e);
            return false;
        }
    }

    private JsonNode doGet(String path) throws IOException {
        Request req = new Request.Builder().url(baseUrl + path).get().build();
        try (Response resp = http.newCall(req).execute()) {
            if (resp.code() == 401) { login(); return doGet(path); }
            return mapper.readTree(resp.body().string());
        }
    }

    private JsonNode doPost(String path, String jsonBody) throws IOException {
        RequestBody rb = (jsonBody != null)
                ? RequestBody.create(jsonBody, JSON)
                : RequestBody.create("", JSON);
        Request req = new Request.Builder().url(baseUrl + path).post(rb).build();
        try (Response resp = http.newCall(req).execute()) {
            if (resp.code() == 401) { login(); return doPost(path, jsonBody); }
            return mapper.readTree(resp.body().string());
        }
    }

    public List<JsonNode> getInbounds() {
        try {
            JsonNode data = doGet("/xui/inbound/list");
            ArrayNode arr = (ArrayNode) data.path("obj");
            List<JsonNode> list = new ArrayList<>();
            arr.forEach(list::add);
            return list;
        } catch (IOException e) {
            log.error("getInbounds failed", e);
            return List.of();
        }
    }

    public JsonNode getClientStats(String email) {
        try {
            JsonNode data = doGet("/xui/inbound/getClientTraffics/" + email);
            return data.path("obj").isNull() ? null : data.path("obj");
        } catch (IOException e) {
            log.error("getClientStats failed", e);
            return null;
        }
    }

    public record AddResult(boolean success, String clientId) {}

    public AddResult addClient(int inboundId, String email, String remark,
                               long expiryTime, int totalGb) {
        String clientId = UUID.randomUUID().toString();
        long totalBytes = totalGb > 0 ? (long) totalGb * 1024 * 1024 * 1024 : 0;

        ObjectNode client = mapper.createObjectNode();
        client.put("id", clientId);
        client.put("email", email);
        client.put("enable", true);
        client.put("expiryTime", expiryTime);
        client.put("totalGB", totalBytes);
        client.put("remark", remark != null ? remark : "");
        client.put("flow", "");

        ArrayNode clients = mapper.createArrayNode();
        clients.add(client);

        ObjectNode settings = mapper.createObjectNode();
        settings.set("clients", clients);

        ObjectNode payload = mapper.createObjectNode();
        payload.put("id", inboundId);
        payload.set("settings", settings);

        try {
            JsonNode resp = doPost("/xui/inbound/addClient", payload.toString());
            boolean ok = resp.path("success").asBoolean(false);
            return new AddResult(ok, clientId);
        } catch (IOException e) {
            log.error("addClient failed", e);
            return new AddResult(false, clientId);
        }
    }

    public boolean deleteClient(int inboundId, String clientId) {
        try {
            JsonNode resp = doPost("/xui/inbound/" + inboundId + "/delClient/" + clientId, null);
            return resp.path("success").asBoolean(false);
        } catch (IOException e) {
            log.error("deleteClient failed", e);
            return false;
        }
    }

    public boolean resetClientTraffic(int inboundId, String email) {
        try {
            JsonNode resp = doPost("/xui/inbound/" + inboundId + "/resetClientTraffic/" + email, null);
            return resp.path("success").asBoolean(false);
        } catch (IOException e) {
            log.error("resetClientTraffic failed", e);
            return false;
        }
    }
}
