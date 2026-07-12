package greencloud.impl.managers.alt;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import greencloud.GreenCloud;
import net.minecraft.util.Session;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.awt.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.stream.Collectors;


public class MicrosoftLogin {
    private static final String CLIENT_ID = "42a60a84-599d-44b2-a7c6-b00cdef1d6a2";
    private static final int PORT = 25575;
    private static final String REDIRECT_URI = "http://localhost:" + PORT + "/callback";
    private static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    private static HttpServer activeServer = null;

    public static void login(Consumer<String> statusCallback) {
        if (activeServer != null) activeServer.stop(0);
        String state = UUID.randomUUID().toString().substring(0, 8);
        URI uri = getMSAuthLink(state);
        try {
            if (!greencloud.impl.utils.AndroidUtil.isAndroid() && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(uri);
            } else if (!greencloud.impl.utils.AndroidUtil.isAndroid()) {
                Runtime.getRuntime().exec("rundll32 url.dll,FileProtocolHandler " + uri.toString());
            } else {
                try {
                    Class<?> uriClass    = Class.forName("android.net.Uri");
                    Object   androidUri  = uriClass.getMethod("parse", String.class).invoke(null, uri.toString());
                    Class<?> intentClass = Class.forName("android.content.Intent");
                    Object   intent      = intentClass.getConstructor(String.class, uriClass)
                                              .newInstance("android.intent.action.VIEW", androidUri);
                    intentClass.getMethod("addFlags", int.class).invoke(intent, 0x10000000); // FLAG_ACTIVITY_NEW_TASK
                    Class<?> threadClass = Class.forName("android.app.ActivityThread");
                    Object   app         = threadClass.getMethod("currentApplication").invoke(null);
                    app.getClass().getMethod("startActivity", intentClass).invoke(app, intent);
                } catch (Exception ex) {
                    if (statusCallback != null) statusCallback.accept("Open browser: " + uri);
                }
            }
        } catch (Exception e) { return; }

        acquireMSAuthCode(state)
                .thenCompose(MicrosoftLogin::acquireMSAccessTokens)
                .thenCompose(tokens -> {
                    String refresh = tokens.get("refresh_token");
                    return acquireXboxAccessToken(tokens.get("access_token")).thenApply(xbox -> {
                        Map<String, String> m = new HashMap<>();
                        m.put("xbox_token", xbox);
                        m.put("refresh_token", refresh);
                        return m;
                    });
                })
                .thenCompose(d -> acquireXboxXstsToken(d.get("xbox_token")).thenApply(x -> { d.putAll(x); return d; }))
                .thenCompose(d -> acquireMCAccessToken(d.get("Token"), d.get("uhs")).thenApply(mc -> { d.put("mc_token", mc); return d; }))
                .thenCompose(d -> loginToMinecraft(d.get("mc_token")).thenApply(s -> {
                    Alt alt = new Alt(d.get("mc_token"), s.getUsername(), Alt.AccountType.MICROSOFT, Alt.Status.LoggedIn);
                    alt.setRefreshToken(d.get("refresh_token"));
                    GreenCloud.altManager.addAlt(alt);
                    GreenCloud.altManager.login(alt);
                    return s;
                }))
                .exceptionally(e -> null);
    }

    public static URI getMSAuthLink(String state) {
        try {
            return new URIBuilder("https://login.live.com/oauth20_authorize.srf")
                    .addParameter("client_id", CLIENT_ID)
                    .addParameter("response_type", "code")
                    .addParameter("redirect_uri", REDIRECT_URI)
                    .addParameter("scope", "XboxLive.signin XboxLive.offline_access")
                    .addParameter("state", state)
                    .addParameter("prompt", "select_account").build();
        } catch (Exception e) { return null; }
    }

    public static CompletableFuture<String> acquireMSAuthCode(String state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                activeServer = HttpServer.create(new InetSocketAddress(PORT), 0);
                CountDownLatch latch = new CountDownLatch(1);
                AtomicReference<String> code = new AtomicReference<>(null);
                activeServer.createContext("/callback", exchange -> {
                    String q = exchange.getRequestURI().toString();
                    if (q.contains("?")) {
                        Map<String, String> m = URLEncodedUtils.parse(q.substring(q.indexOf("?") + 1), StandardCharsets.UTF_8)
                                .stream().collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));
                        if (state.equals(m.get("state"))) code.set(m.get("code"));
                    }
                    String res = "<html><body><h1>Done</h1></body></html>";
                    exchange.sendResponseHeaders(200, res.length());
                    exchange.getResponseBody().write(res.getBytes());
                    exchange.getResponseBody().close();
                    latch.countDown();
                });
                activeServer.start();
                latch.await(2, TimeUnit.MINUTES);
                activeServer.stop(0);
                return code.get();
            } catch (Exception e) { throw new CompletionException(e); }
        }, EXECUTOR);
    }

    public static CompletableFuture<Map<String, String>> acquireMSAccessTokens(String code) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost req = new HttpPost("https://login.live.com/oauth20_token.srf");
                List<NameValuePair> p = new ArrayList<>();
                p.add(new BasicNameValuePair("client_id", CLIENT_ID));
                p.add(new BasicNameValuePair("grant_type", "authorization_code"));
                p.add(new BasicNameValuePair("code", code));
                p.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
                req.setEntity(new UrlEncodedFormEntity(p));
                JsonObject j = new JsonParser().parse(EntityUtils.toString(client.execute(req).getEntity())).getAsJsonObject();
                Map<String, String> r = new HashMap<>();
                r.put("access_token", j.get("access_token").getAsString());
                r.put("refresh_token", j.get("refresh_token").getAsString());
                return r;
            } catch (Exception e) { throw new CompletionException(e); }
        }, EXECUTOR);
    }

    public static CompletableFuture<String> acquireXboxAccessToken(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost req = new HttpPost("https://user.auth.xboxlive.com/user/authenticate");
                JsonObject props = new JsonObject();
                props.addProperty("AuthMethod", "RPS");
                props.addProperty("SiteName", "user.auth.xboxlive.com");
                props.addProperty("RpsTicket", "d=" + token);
                JsonObject b = new JsonObject();
                b.add("Properties", props);
                b.addProperty("RelyingParty", "http://auth.xboxlive.com");
                b.addProperty("TokenType", "JWT");
                req.setEntity(new StringEntity(b.toString()));
                req.setHeader("Content-Type", "application/json");
                return new JsonParser().parse(EntityUtils.toString(client.execute(req).getEntity())).getAsJsonObject().get("Token").getAsString();
            } catch (Exception e) { throw new CompletionException(e); }
        }, EXECUTOR);
    }

    public static CompletableFuture<Map<String, String>> acquireXboxXstsToken(String xbox) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost req = new HttpPost("https://xsts.auth.xboxlive.com/xsts/authorize");
                JsonObject props = new JsonObject();
                props.addProperty("SandboxId", "RETAIL");
                JsonArray ut = new JsonArray();
                ut.add(new JsonPrimitive(xbox));
                props.add("UserTokens", ut);
                JsonObject b = new JsonObject();
                b.add("Properties", props);
                b.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
                b.addProperty("TokenType", "JWT");
                req.setEntity(new StringEntity(b.toString()));
                req.setHeader("Content-Type", "application/json");
                JsonObject j = new JsonParser().parse(EntityUtils.toString(client.execute(req).getEntity())).getAsJsonObject();
                Map<String, String> r = new HashMap<>();
                r.put("Token", j.get("Token").getAsString());
                r.put("uhs", j.get("DisplayClaims").getAsJsonObject().get("xui").getAsJsonArray().get(0).getAsJsonObject().get("uhs").getAsString());
                return r;
            } catch (Exception e) { throw new CompletionException(e); }
        }, EXECUTOR);
    }

    public static CompletableFuture<String> acquireMCAccessToken(String xsts, String uhs) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost req = new HttpPost("https://api.minecraftservices.com/authentication/login_with_xbox");
                JsonObject b = new JsonObject();
                b.addProperty("identityToken", "XBL3.0 x=" + uhs + ";" + xsts);
                req.setEntity(new StringEntity(b.toString()));
                req.setHeader("Content-Type", "application/json");
                return new JsonParser().parse(EntityUtils.toString(client.execute(req).getEntity())).getAsJsonObject().get("access_token").getAsString();
            } catch (Exception e) { throw new CompletionException(e); }
        }, EXECUTOR);
    }

    public static CompletableFuture<Session> loginToMinecraft(String token) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpGet req = new HttpGet("https://api.minecraftservices.com/minecraft/profile");
                req.setHeader("Authorization", "Bearer " + token);
                JsonObject j = new JsonParser().parse(EntityUtils.toString(client.execute(req).getEntity())).getAsJsonObject();
                return new Session(j.get("name").getAsString(), j.get("id").getAsString(), token, "mojang");
            } catch (Exception e) { throw new CompletionException(e); }
        }, EXECUTOR);
    }

    public static CompletableFuture<Map<String, String>> refresh(String refresh) {
        return CompletableFuture.supplyAsync(() -> {
            try (CloseableHttpClient client = HttpClients.createDefault()) {
                HttpPost req = new HttpPost("https://login.live.com/oauth20_token.srf");
                List<NameValuePair> p = new ArrayList<>();
                p.add(new BasicNameValuePair("client_id", CLIENT_ID));
                p.add(new BasicNameValuePair("refresh_token", refresh));
                p.add(new BasicNameValuePair("grant_type", "refresh_token"));
                p.add(new BasicNameValuePair("redirect_uri", REDIRECT_URI));
                req.setEntity(new UrlEncodedFormEntity(p));
                JsonObject j = new JsonParser().parse(EntityUtils.toString(client.execute(req).getEntity())).getAsJsonObject();
                Map<String, String> r = new HashMap<>();
                r.put("access_token", j.get("access_token").getAsString());
                r.put("refresh_token", j.get("refresh_token").getAsString());
                return r;
            } catch (Exception e) { return null; }
        }, EXECUTOR);
    }
}