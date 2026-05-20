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
import java.io.FileInputStream;
import java.io.IOException;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
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
        String url = Settings.get().XUI_URL;
        if (url == null || url.isBlank()) {
            this.baseUrl = null;
            this.http = null;
        } else {
            this.baseUrl = url.replaceAll("/+$", "");
            this.http = buildClient();
        }
    }

    private void ensureEnabled() {
        if (baseUrl == null) {
            throw new XuiApiException("XUI panel is not configured");
        }
    }

    public static XuiClient get() { return INSTANCE; }

    private OkHttpClient buildClient() {
        OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .cookieJar(new CookieJar() {
                    private final List<Cookie> cookies = new ArrayList<>();
                    public void saveFromResponse(HttpUrl url, List<Cookie> c) { cookies.addAll(c); }
                    public List<Cookie> loadForRequest(HttpUrl url) { return cookies; }
                });

        String certPath = Settings.get().XUI_CERT_PATH;
        if (certPath != null && !certPath.isBlank()) {
            try {
                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                ks.load(null, null);
                try (FileInputStream is = new FileInputStream(certPath)) {
                    CertificateFactory cf = CertificateFactory.getInstance("X.509");
                    ks.setCertificateEntry("xui", cf.generateCertificate(is));
                }
                TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                SSLContext sc = SSLContext.getInstance("TLS");
                sc.init(null, tmf.getTrustManagers(), null);
                builder.sslSocketFactory(sc.getSocketFactory(), (X509TrustManager) tmf.getTrustManagers()[0]);
            } catch (Exception e) {
                throw new RuntimeException("Failed to load custom XUI certificate from " + certPath, e);
            }
        }
        return builder.build();
    }

    public void login() {
        ensureEnabled();
        String body = "username=" + Settings.get().XUI_USERNAME +
                      "&password=" + Settings.get().XUI_PASSWORD;
        Request req = new Request.Builder()
                .url(baseUrl + "/login")
                .post(RequestBody.create(body, FORM))
                .build();
        try (Response resp = http.newCall(req).execute()) {
            JsonNode node = mapper.readTree(resp.body().string());
            if (!node.path("success").asBoolean(false)) {
                throw new XuiApiException("XUI login rejected");
            }
        } catch (IOException e) {
            throw new XuiApiException("XUI login failed", e);
        }
    }

    private JsonNode doRequest(Request req) throws IOException {
        try (Response resp = http.newCall(req).execute()) {
            if (resp.code() == 401) {
                login();
                try (Response resp2 = http.newCall(req).execute()) {
                    return mapper.readTree(resp2.body().string());
                }
            }
            return mapper.readTree(resp.body().string());
        }
    }

    public List<JsonNode> getInbounds() {
        ensureEnabled();
        try {
            Request req = new Request.Builder().url(baseUrl + "/xui/inbound/list").get().build();
            JsonNode data = doRequest(req);
            ArrayNode arr = (ArrayNode) data.path("obj");
            List<JsonNode> list = new ArrayList<>();
            arr.forEach(list::add);
            return list;
        } catch (IOException e) {
            throw new XuiApiException("Failed to fetch inbounds", e);
        }
    }

    public JsonNode getClientStats(String email) {
        ensureEnabled();
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/xui/inbound/getClientTraffics/" + email)
                    .get().build();
            JsonNode data = doRequest(req);
            return data.path("obj").isNull() ? null : data.path("obj");
        } catch (IOException e) {
            throw new XuiApiException("Failed to fetch client stats for " + email, e);
        }
    }

    public record AddResult(boolean success, String clientId) {}

    public AddResult addClient(int inboundId, String email, String remark,
                               long expiryTime, int totalGb) {
        String clientId = UUID.randomUUID().toString();
        ensureEnabled();
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
            Request req = new Request.Builder()
                    .url(baseUrl + "/xui/inbound/addClient")
                    .post(RequestBody.create(payload.toString(), JSON))
                    .build();
            JsonNode resp = doRequest(req);
            boolean ok = resp.path("success").asBoolean(false);
            return new AddResult(ok, clientId);
        } catch (IOException e) {
            throw new XuiApiException("Failed to add client", e);
        }
    }

    public boolean deleteClient(int inboundId, String clientId) {
        ensureEnabled();
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/xui/inbound/" + inboundId + "/delClient/" + clientId)
                    .post(RequestBody.create("", JSON))
                    .build();
            JsonNode resp = doRequest(req);
            return resp.path("success").asBoolean(false);
        } catch (IOException e) {
            throw new XuiApiException("Failed to delete client", e);
        }
    }

    public boolean resetClientTraffic(int inboundId, String email) {
        ensureEnabled();
        try {
            Request req = new Request.Builder()
                    .url(baseUrl + "/xui/inbound/" + inboundId + "/resetClientTraffic/" + email)
                    .post(RequestBody.create("", JSON))
                    .build();
            JsonNode resp = doRequest(req);
            return resp.path("success").asBoolean(false);
        } catch (IOException e) {
            throw new XuiApiException("Failed to reset client traffic", e);
        }
    }
}
