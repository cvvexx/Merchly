package io.cvvexxx.frontend.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.frontend.client.product.internal.ProductsInternalRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Фиксирует разделение трафика после включения api-gateway.
 *
 * <p>Подняты два независимых WireMock. Первый играет роль шлюза, второй - роль
 * сервисов, доступных напрямую. Публичные вызовы (product, user, order, review)
 * обязаны уходить в шлюз; внутренние ручки {@code /api/internal/**} шлюз наружу не
 * публикует, поэтому такие вызовы обязаны идти мимо него.
 *
 * <p>Если кто-то переведёт внутренний клиент на шлюз (или забудет перевести публичный),
 * соответствующий тест упадёт.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class GatewayTrafficRoutingIT {

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    /** Играет роль api-gateway. */
    static WireMockServer gateway = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    /** Играет роль сервисов, к которым фронт ходит напрямую (внутренние ручки, Keycloak). */
    static WireMockServer direct = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductsInternalRestClient productsInternalRestClient;

    @Autowired
    private UserInternalRestClient userInternalRestClient;

    @BeforeAll
    static void startStubs() {
        gateway.start();
        direct.start();
    }

    @AfterAll
    static void stopStubs() {
        gateway.stop();
        direct.stop();
    }

    @AfterEach
    void resetStubs() {
        gateway.resetAll();
        direct.resetAll();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.restclient.uri.api_gateway", gateway::baseUrl);
        registry.add("spring.restclient.uri.product_service", direct::baseUrl);
        registry.add("spring.restclient.uri.user_service", direct::baseUrl);
        registry.add("spring.restclient.uri.reviews_service", direct::baseUrl);
        registry.add("spring.restclient.uri.orders_service", direct::baseUrl);
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri",
                () -> direct.baseUrl() + "/token");
    }

    @Nested
    @DisplayName("Публичный трафик уходит в шлюз")
    class PublicTrafficGoesThroughGateway {

        @Test
        @DisplayName("каталог товаров: GET /api/products - в шлюз, напрямую в product-service не идёт")
        void productCatalogue() throws Exception {
            UUID productId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            gateway.stubFor(WireMock.get(urlPathEqualTo("/api/products"))
                    .willReturn(okJson("""
                            [{"id":"%s","title":"Худи","description":"desc","quantity":5,"price":19.99,
                              "imageFileName":"hoodie.png","createdBy":"%s"}]
                            """.formatted(productId, ownerId))));
            gateway.stubFor(WireMock.post(urlPathEqualTo("/api/reviews/products/stats"))
                    .willReturn(okJson("[]")));
            direct.stubFor(WireMock.get(urlPathEqualTo("/api/internal/users"))
                    .willReturn(okJson("""
                            [{"id":"%s","username":"ink_studio","avatarFileName":"avatar.png"}]
                            """.formatted(ownerId))));
            stubClientCredentialsToken();

            mockMvc.perform(get("/catalogue/products/list").with(authentication(fakeUserToken())))
                    .andExpect(status().isOk());

            gateway.verify(getRequestedFor(urlPathEqualTo("/api/products")));
            direct.verify(0, getRequestedFor(urlPathEqualTo("/api/products")));
        }

        @Test
        @DisplayName("статистика отзывов: POST /api/reviews/products/stats - в шлюз")
        void reviewStats() throws Exception {
            UUID productId = UUID.randomUUID();
            UUID ownerId = UUID.randomUUID();

            gateway.stubFor(WireMock.get(urlPathEqualTo("/api/products"))
                    .willReturn(okJson("""
                            [{"id":"%s","title":"Худи","description":"desc","quantity":5,"price":19.99,
                              "imageFileName":"hoodie.png","createdBy":"%s"}]
                            """.formatted(productId, ownerId))));
            gateway.stubFor(WireMock.post(urlPathEqualTo("/api/reviews/products/stats"))
                    .willReturn(okJson("""
                            [{"productId":"%s","averageRating":4.5,"totalReviews":10}]
                            """.formatted(productId))));
            direct.stubFor(WireMock.get(urlPathEqualTo("/api/internal/users"))
                    .willReturn(okJson("""
                            [{"id":"%s","username":"ink_studio","avatarFileName":"avatar.png"}]
                            """.formatted(ownerId))));
            stubClientCredentialsToken();

            mockMvc.perform(get("/catalogue/products/list").with(authentication(fakeUserToken())))
                    .andExpect(status().isOk());

            gateway.verify(postRequestedFor(urlPathEqualTo("/api/reviews/products/stats")));
            direct.verify(0, postRequestedFor(urlPathEqualTo("/api/reviews/products/stats")));
        }

        @Test
        @DisplayName("корзина пользователя: GET /api/users/cart - в шлюз")
        void userCart() throws Exception {
            gateway.stubFor(WireMock.get(urlPathEqualTo("/api/users/cart")).willReturn(okJson("[]")));
            stubClientCredentialsToken();

            mockMvc.perform(get("/cart").with(authentication(fakeUserToken())));

            gateway.verify(getRequestedFor(urlPathEqualTo("/api/users/cart")));
            direct.verify(0, getRequestedFor(urlPathEqualTo("/api/users/cart")));
        }

        @Test
        @DisplayName("заказы пользователя: GET /api/orders/ - в шлюз")
        void userOrders() throws Exception {
            gateway.stubFor(WireMock.get(urlPathEqualTo("/api/orders/")).willReturn(okJson("[]")));
            stubClientCredentialsToken();

            mockMvc.perform(get("/orders").with(authentication(fakeUserToken())));

            gateway.verify(getRequestedFor(urlPathEqualTo("/api/orders/")));
            direct.verify(0, getRequestedFor(urlPathEqualTo("/api/orders/")));
        }

        @Test
        @DisplayName("токен пользователя доезжает до шлюза в заголовке Authorization")
        void userTokenIsForwarded() throws Exception {
            gateway.stubFor(WireMock.get(urlPathEqualTo("/api/products")).willReturn(okJson("[]")));
            stubClientCredentialsToken();

            mockMvc.perform(get("/catalogue/products/list").with(authentication(fakeUserToken())))
                    .andExpect(status().isOk());

            gateway.verify(getRequestedFor(urlPathEqualTo("/api/products"))
                    .withHeader("Authorization", matching("Bearer .+")));
        }
    }

    @Nested
    @DisplayName("Внутренний трафик идёт мимо шлюза")
    class InternalTrafficBypassesGateway {

        @Test
        @DisplayName("/api/internal/products - напрямую в product-service")
        void internalProducts() {
            UUID productId = UUID.randomUUID();
            direct.stubFor(WireMock.get(urlPathEqualTo("/api/internal/products"))
                    .willReturn(okJson("""
                            [{"id":"%s","title":"Худи","description":"desc","quantity":1,"price":10,
                              "imageFileName":null,"createdBy":"%s"}]
                            """.formatted(productId, UUID.randomUUID()))));
            stubClientCredentialsToken();

            productsInternalRestClient.findAllProductsByIds(List.of(productId));

            direct.verify(getRequestedFor(urlPathEqualTo("/api/internal/products")));
            gateway.verify(0, getRequestedFor(urlPathEqualTo("/api/internal/products")));
        }

        @Test
        @DisplayName("/api/internal/users - напрямую в user-service")
        void internalUsers() {
            UUID userId = UUID.randomUUID();
            direct.stubFor(WireMock.get(urlPathEqualTo("/api/internal/users"))
                    .willReturn(okJson("""
                            [{"id":"%s","username":"ink_studio","avatarFileName":"avatar.png"}]
                            """.formatted(userId))));
            stubClientCredentialsToken();

            userInternalRestClient.findAllUsersByIds(List.of(userId));

            direct.verify(getRequestedFor(urlPathEqualTo("/api/internal/users")));
            gateway.verify(0, getRequestedFor(urlPathEqualTo("/api/internal/users")));
        }
    }

    private void stubClientCredentialsToken() {
        direct.stubFor(WireMock.post(urlPathEqualTo("/token"))
                .willReturn(okJson("""
                        {"access_token":"service-token","token_type":"Bearer","expires_in":3600}
                        """)));
    }

    private KeycloakJwtAuthenticationToken fakeUserToken() {
        UUID userId = UUID.randomUUID();
        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        String accessToken = fakeJwt(userId, Instant.now().plus(1, ChronoUnit.HOURS));
        return new KeycloakJwtAuthenticationToken(
                userId.toString(), userId, accessToken, "refresh-token", authorities);
    }

    private static String fakeJwt(UUID subject, Instant expiresAt) {
        String header = base64("{\"alg\":\"none\"}");
        String payload = base64("{\"sub\":\"" + subject + "\",\"exp\":" + expiresAt.getEpochSecond() + "}");
        return header + "." + payload + ".signature";
    }

    private static String base64(String value) {
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }
}
