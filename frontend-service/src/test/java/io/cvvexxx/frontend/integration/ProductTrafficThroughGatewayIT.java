package io.cvvexxx.frontend.integration;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.frontend.client.product.internal.ProductsInternalRestClient;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
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


@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class ProductTrafficThroughGatewayIT {

    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));

    static WireMockServer gateway = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static WireMockServer productService = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static WireMockServer otherServices = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductsInternalRestClient productsInternalRestClient;

    @BeforeAll
    static void startStubs() {
        gateway.start();
        productService.start();
        otherServices.start();
    }

    @AfterAll
    static void stopStubs() {
        gateway.stop();
        productService.stop();
        otherServices.stop();
    }

    @AfterEach
    void resetStubs() {
        gateway.resetAll();
        productService.resetAll();
        otherServices.resetAll();
    }

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        registry.add("spring.restclient.uri.api_gateway", gateway::baseUrl);
        registry.add("spring.restclient.uri.product_service", productService::baseUrl);
        registry.add("spring.restclient.uri.user_service", otherServices::baseUrl);
        registry.add("spring.restclient.uri.reviews_service", otherServices::baseUrl);
        registry.add("spring.security.oauth2.client.provider.keycloak.token-uri",
                () -> otherServices.baseUrl() + "/token");
    }

    @Test
    @DisplayName("список каталога фронт запрашивает у шлюза, а не напрямую у product-service")
    void productList_GoesThroughGateway() throws Exception {
        UUID productId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        gateway.stubFor(WireMock.get(urlPathEqualTo("/api/products"))
                .willReturn(okJson("""
                        [{"id":"%s","title":"Худи","description":"desc","quantity":5,"price":19.99,
                          "imageFileName":"hoodie.png","createdBy":"%s"}]
                        """.formatted(productId, ownerId))));
        otherServices.stubFor(WireMock.get(urlPathEqualTo("/api/internal/users"))
                .willReturn(okJson("""
                        [{"id":"%s","username":"ink_studio","avatarFileName":"avatar.png"}]
                        """.formatted(ownerId))));
        otherServices.stubFor(WireMock.post(urlPathEqualTo("/api/reviews/products/stats"))
                .willReturn(okJson("[]")));
        stubClientCredentialsToken();

        mockMvc.perform(get("/catalogue/products/list").with(authentication(fakeUserToken())))
                .andExpect(status().isOk());

        gateway.verify(getRequestedFor(urlPathEqualTo("/api/products")));

        productService.verify(0, getRequestedFor(urlPathEqualTo("/api/products")));
    }

    @Test
    @DisplayName("шлюз проксирует Authorization: токен пользователя доезжает до product-service")
    void userToken_ReachesGateway() throws Exception {
        gateway.stubFor(WireMock.get(urlPathEqualTo("/api/products")).willReturn(okJson("[]")));
        stubClientCredentialsToken();

        mockMvc.perform(get("/catalogue/products/list").with(authentication(fakeUserToken())))
                .andExpect(status().isOk());

        gateway.verify(getRequestedFor(urlPathEqualTo("/api/products"))
                .withHeader("Authorization", matching("Bearer .+")));
    }

    @Test
    @DisplayName("внутренний вызов /api/internal/products идёт мимо шлюза, напрямую в product-service")
    void internalCalls_BypassGateway() {
        UUID productId = UUID.randomUUID();
        productService.stubFor(WireMock.get(urlPathEqualTo("/api/internal/products"))
                .willReturn(okJson("""
                        [{"id":"%s","title":"Худи","description":"desc","quantity":1,"price":10,
                          "imageFileName":null,"createdBy":"%s"}]
                        """.formatted(productId, UUID.randomUUID()))));
        stubClientCredentialsToken();

        productsInternalRestClient.findAllProductsByIds(List.of(productId));


        productService.verify(getRequestedFor(urlPathEqualTo("/api/internal/products")));

        gateway.verify(0, getRequestedFor(urlPathEqualTo("/api/internal/products")));
    }

    private void stubClientCredentialsToken() {
        otherServices.stubFor(WireMock.post(urlPathEqualTo("/token"))
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
