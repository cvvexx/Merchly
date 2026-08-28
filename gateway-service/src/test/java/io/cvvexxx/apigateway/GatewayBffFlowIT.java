package io.cvvexxx.apigateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cvvexxx.apigateway.client.KeycloakRestClient;
import io.cvvexxx.apigateway.dto.KeycloakTokenResponse;
import io.cvvexxx.apigateway.support.RedisBackedGatewayIT;
import io.cvvexxx.apigateway.support.StubJwtDecoderConfiguration;
import io.cvvexxx.apigateway.support.TestTokens;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StubJwtDecoderConfiguration.class)
@ActiveProfiles("test")
class GatewayBffFlowIT extends RedisBackedGatewayIT {

    private static final Pattern CSRF_FIELD =
            Pattern.compile("name=\"_csrf\"\\s+value=\"([^\"]+)\"");

    static WireMockServer frontend =
            new WireMockServer(WireMockConfiguration.options().dynamicPort().http2PlainDisabled(true));

    @LocalServerPort
    int gatewayPort;

    @MockitoBean
    KeycloakRestClient keycloakRestClient;

    @BeforeAll
    static void startFrontend() {
        frontend.start();
    }

    @AfterAll
    static void stopFrontend() {
        frontend.stop();
    }

    @DynamicPropertySource
    static void frontendUri(DynamicPropertyRegistry registry) {
        registry.add("FRONTEND_SERVICE_URI", () -> "http://localhost:" + frontend.port());
    }

    @AfterEach
    void resetStubs() {
        frontend.resetAll();
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + gatewayPort + path);
    }

    @Test
    @DisplayName("страницу входа отдаёт сам шлюз, а не frontend-service")
    void loginPage_IsServedByGateway() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Вход в аккаунт");
        frontend.verify(0, getRequestedFor(urlPathMatching("/login.*")));
    }

    @Test
    @DisplayName("fallback отдаёт сам шлюз: маршрут на фронт его не перехватывает")
    void fallback_IsServedByGateway() throws Exception {
        HttpClient client = HttpClient.newHttpClient();

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/fallback/products")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(503);
        assertThat(response.body()).contains("product_service_unavailable");
        frontend.verify(0, getRequestedFor(urlPathMatching("/fallback/.*")));
    }

    @Test
    @DisplayName("статика уходит на frontend-service и не требует входа")
    void staticResources_AreProxiedAnonymously() throws Exception {
        frontend.stubFor(get(urlPathEqualTo("/css/style.css"))
                .willReturn(aResponse().withStatus(200).withBody(".rail-brand{}")));

        HttpResponse<String> response = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/css/style.css")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(200);
        frontend.verify(getRequestedFor(urlPathEqualTo("/css/style.css"))
                .withHeader("Authorization", absent()));
    }

    @Test
    @DisplayName("страница без входа ведёт на /login, а не на frontend-service")
    void protectedPage_WithoutSession_RedirectsToLogin() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/catalogue/products/list")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElseThrow()).contains("/login");
        frontend.verify(0, getRequestedFor(urlPathMatching("/catalogue/.*")));
    }

    @Test
    @DisplayName("после входа шлюз подставляет в запрос к фронту Bearer из своей сессии")
    void afterLogin_GatewayRelaysBearerToFrontend() throws Exception {
        String accessToken = TestTokens.user();
        when(keycloakRestClient.login("johndoe", "password"))
                .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
        frontend.stubFor(get(urlPathEqualTo("/catalogue/products/list"))
                .willReturn(aResponse().withStatus(200).withBody("<html>каталог</html>")));

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();

        String csrf = csrfTokenFromLoginPage(client);

        HttpResponse<String> login = client.send(
                HttpRequest.newBuilder(uri("/do-login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form(
                                "login", "johndoe",
                                "password", "password",
                                "_csrf", csrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(login.statusCode()).isEqualTo(302);
        assertThat(login.headers().firstValue("Location").orElseThrow())
                .endsWith("/catalogue/products/list");

        HttpResponse<String> page = client.send(
                HttpRequest.newBuilder(uri("/catalogue/products/list")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(page.statusCode()).isEqualTo(200);
        assertThat(page.body()).contains("каталог");
        frontend.verify(getRequestedFor(urlPathEqualTo("/catalogue/products/list"))
                .withHeader("Authorization", equalTo("Bearer " + accessToken)));
    }

    @Test
    @DisplayName("свой Bearer из браузера не проходит: наружу уезжает только токен из сессии")
    void browserSuppliedBearer_IsNotRelayed() throws Exception {
        frontend.stubFor(get(urlPathEqualTo("/css/style.css"))
                .willReturn(aResponse().withStatus(200).withBody(".rail-brand{}")));

        HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(uri("/css/style.css"))
                        .header("Authorization", "Bearer forged-token")
                        .GET().build(),
                HttpResponse.BodyHandlers.ofString());

        frontend.verify(getRequestedFor(urlPathEqualTo("/css/style.css"))
                .withHeader("Authorization", absent()));
    }

    @Test
    @DisplayName("проксируемая form-POST доезжает до фронта с телом, а не пустой")
    void proxiedFormPost_KeepsBody() throws Exception {
        String accessToken = TestTokens.user();
        when(keycloakRestClient.login("johndoe", "password"))
                .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
        frontend.stubFor(post(urlPathEqualTo("/profile/edit"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/profile")));

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String csrf = csrfTokenFromLoginPage(client);
        client.send(HttpRequest.newBuilder(uri("/do-login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form(
                                "login", "johndoe", "password", "password", "_csrf", csrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        client.send(HttpRequest.newBuilder(uri("/profile/edit"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form(
                                "firstname", "Иван", "_csrf", csrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        frontend.verify(postRequestedFor(urlPathEqualTo("/profile/edit"))
                .withRequestBody(containing("firstname"))
                .withHeader("Authorization", equalTo("Bearer " + accessToken)));
    }

    @Test
    @DisplayName("проксируемая multipart-форма доезжает до фронта")
    void proxiedMultipartPost_IsAccepted() throws Exception {
        String accessToken = TestTokens.user();
        when(keycloakRestClient.login("johndoe", "password"))
                .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));
        frontend.stubFor(post(urlPathEqualTo("/profile/edit"))
                .willReturn(aResponse().withStatus(302).withHeader("Location", "/profile")));

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String csrf = csrfTokenFromLoginPage(client);
        client.send(HttpRequest.newBuilder(uri("/do-login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form(
                                "login", "johndoe", "password", "password", "_csrf", csrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String boundary = "----MerchlyTestBoundary";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"firstname\"\r\n\r\n"
                + "Ivan\r\n"
                + "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"userAvatar\"; filename=\"a.png\"\r\n"
                + "Content-Type: image/png\r\n\r\n"
                + "fake-png\r\n"
                + "--" + boundary + "--\r\n";

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/profile/edit?_csrf=" + csrf))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode())
                .as("шлюз не должен отклонять multipart-форму, ответ: %s", response.body())
                .isEqualTo(302);
        frontend.verify(postRequestedFor(urlPathEqualTo("/profile/edit"))
                .withRequestBody(containing("firstname")));
    }

    @Test
    @DisplayName("в multipart CSRF-токен из тела не читается — он обязан быть в query")
    void multipartWithCsrfInBody_IsRejected() throws Exception {
        String accessToken = TestTokens.user();
        when(keycloakRestClient.login("johndoe", "password"))
                .thenReturn(new KeycloakTokenResponse(accessToken, "refresh-token", 3600, 7200));

        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        String csrf = csrfTokenFromLoginPage(client);
        client.send(HttpRequest.newBuilder(uri("/do-login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form(
                                "login", "johndoe", "password", "password", "_csrf", csrf)))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        String boundary = "----MerchlyTestBoundary";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"_csrf\"\r\n\r\n"
                + csrf + "\r\n"
                + "--" + boundary + "--\r\n";

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(uri("/profile/edit"))
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofString(body))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("Location").orElseThrow()).contains("/error-403");
        frontend.verify(0, postRequestedFor(urlPathEqualTo("/profile/edit")));
    }

    private String csrfTokenFromLoginPage(HttpClient client) throws IOException, InterruptedException {
        HttpResponse<String> loginPage = client.send(
                HttpRequest.newBuilder(uri("/login")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        Matcher matcher = CSRF_FIELD.matcher(loginPage.body());
        assertThat(matcher.find()).as("на странице входа должно быть скрытое поле _csrf").isTrue();
        return matcher.group(1);
    }

    private String form(String... keyValues) {
        StringBuilder body = new StringBuilder();
        for (int i = 0; i < keyValues.length; i += 2) {
            if (!body.isEmpty()) {
                body.append('&');
            }
            body.append(URLEncoder.encode(keyValues[i], StandardCharsets.UTF_8))
                    .append('=')
                    .append(URLEncoder.encode(keyValues[i + 1], StandardCharsets.UTF_8));
        }
        return body.toString();
    }
}
