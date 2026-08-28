package io.cvvexxx.apigateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import io.cvvexxx.apigateway.support.RedisBackedGatewayIT;
import io.cvvexxx.apigateway.support.StubJwtDecoderConfiguration;
import io.cvvexxx.apigateway.support.TestTokens;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(StubJwtDecoderConfiguration.class)
@ActiveProfiles("test")
class GatewayServiceRoutesIT extends RedisBackedGatewayIT {

    static WireMockServer productService = new WireMockServer(WireMockConfiguration.options().dynamicPort().http2PlainDisabled(true));
    static WireMockServer userService = new WireMockServer(WireMockConfiguration.options().dynamicPort().http2PlainDisabled(true));
    static WireMockServer orderService = new WireMockServer(WireMockConfiguration.options().dynamicPort().http2PlainDisabled(true));
    static WireMockServer reviewService = new WireMockServer(WireMockConfiguration.options().dynamicPort().http2PlainDisabled(true));

    @LocalServerPort
    int gatewayPort;

    @BeforeAll
    static void startDownstreams() {
        productService.start();
        userService.start();
        orderService.start();
        reviewService.start();
    }

    @AfterAll
    static void stopDownstreams() {
        productService.stop();
        userService.stop();
        orderService.stop();
        reviewService.stop();
    }

    @DynamicPropertySource
    static void downstreamUris(DynamicPropertyRegistry registry) {
        registry.add("PRODUCT_SERVICE_URI", () -> "http://localhost:" + productService.port());
        registry.add("USER_SERVICE_URI", () -> "http://localhost:" + userService.port());
        registry.add("ORDER_SERVICE_URI", () -> "http://localhost:" + orderService.port());
        registry.add("REVIEW_SERVICE_URI", () -> "http://localhost:" + reviewService.port());
    }

    @AfterEach
    void resetStubs() {
        productService.resetAll();
        userService.resetAll();
        orderService.resetAll();
        reviewService.resetAll();
    }

    private RestClient gateway() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + gatewayPort)
                .defaultHeader("Authorization", "Bearer " + TestTokens.admin())
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                })
                .build();
    }

    @Test
    @DisplayName("/api/users/** ведёт в user-service и никуда больше")
    void usersRoute() {
        userService.stubFor(get(urlPathEqualTo("/api/users/cart")).willReturn(okJson("[]")));

        ResponseEntity<String> response = gateway().get()
                .uri("/api/users/cart").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        userService.verify(getRequestedFor(urlPathEqualTo("/api/users/cart")));
        productService.verify(0, getRequestedFor(urlPathMatching("/api/users.*")));
        orderService.verify(0, getRequestedFor(urlPathMatching("/api/users.*")));
        reviewService.verify(0, getRequestedFor(urlPathMatching("/api/users.*")));
    }

    @Test
    @DisplayName("/api/orders/** ведёт в order-service и никуда больше")
    void ordersRoute() {
        orderService.stubFor(get(urlPathEqualTo("/api/orders/")).willReturn(okJson("[]")));

        ResponseEntity<String> response = gateway().get()
                .uri("/api/orders/").retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        orderService.verify(getRequestedFor(urlPathEqualTo("/api/orders/")));
        userService.verify(0, getRequestedFor(urlPathMatching("/api/orders.*")));
    }

    @Test
    @DisplayName("/api/reviews/** ведёт в review-service, а не в user-service")
    void reviewsRoute() {
        reviewService.stubFor(post(urlPathEqualTo("/api/reviews/products/stats")).willReturn(okJson("[]")));

        ResponseEntity<String> response = gateway().post()
                .uri("/api/reviews/products/stats")
                .contentType(MediaType.APPLICATION_JSON)
                .body("[]")
                .retrieve().toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        reviewService.verify(postRequestedFor(urlPathEqualTo("/api/reviews/products/stats")));
        userService.verify(0, postRequestedFor(urlPathMatching("/api/reviews.*")));
    }

    @Test
    @DisplayName("/api/products/** ведёт в product-service и никуда больше")
    void productsRoute() {
        productService.stubFor(get(urlPathEqualTo("/api/products")).willReturn(okJson("[]")));

        gateway().get().uri("/api/products").retrieve().toBodilessEntity();

        productService.verify(getRequestedFor(urlPathEqualTo("/api/products")));
        userService.verify(0, getRequestedFor(urlPathMatching("/api/products.*")));
    }

    @Test
    @DisplayName("Host сохраняется на всех маршрутах: Location из 201 указывает на шлюз")
    void hostIsPreservedOnEveryRoute() {
        orderService.stubFor(post(urlPathEqualTo("/api/orders/create"))
                .willReturn(aResponse().withStatus(201)
                        .withHeader("Location", "http://localhost:" + gatewayPort + "/api/orders/new-id")));

        gateway().post().uri("/api/orders/create")
                .contentType(MediaType.APPLICATION_JSON).body("{}")
                .retrieve().toBodilessEntity();

        orderService.verify(postRequestedFor(urlPathEqualTo("/api/orders/create"))
                .withHeader("Host", equalTo("localhost:" + gatewayPort)));
        userService.verify(0, postRequestedFor(urlPathMatching("/api/orders.*")));
    }

    @Test
    @DisplayName("медленный ответ downstream не рубится circuit breaker-ом ни на одном маршруте")
    void slowDownstream_IsNotCutOffByTimeLimiter() {
        int delayMs = 2000;
        productService.stubFor(get(urlPathEqualTo("/api/products"))
                .willReturn(okJson("[]").withFixedDelay(delayMs)));
        userService.stubFor(get(urlPathEqualTo("/api/users/me"))
                .willReturn(okJson("{}").withFixedDelay(delayMs)));
        orderService.stubFor(get(urlPathEqualTo("/api/orders/"))
                .willReturn(okJson("[]").withFixedDelay(delayMs)));
        reviewService.stubFor(get(urlPathEqualTo("/api/reviews/stats"))
                .willReturn(okJson("[]").withFixedDelay(delayMs)));

        for (String path : new String[]{"/api/products", "/api/users/me", "/api/orders/", "/api/reviews/stats"}) {
            ResponseEntity<String> response = gateway().get()
                    .uri(path).retrieve().toEntity(String.class);

            assertThat(response.getStatusCode().value())
                    .as("%s: медленный ответ не должен превращаться в 503", path)
                    .isEqualTo(200);
        }
    }

    @Test
    @DisplayName("внутренние ручки любого сервиса наружу не публикуются")
    void internalEndpointsAreNotExposed() {
        for (String path : new String[]{"/api/internal/products", "/api/internal/users"}) {
            ResponseEntity<String> response = gateway().get()
                    .uri(path + "?ids=1").retrieve().toEntity(String.class);

            assertThat(response.getStatusCode().value())
                    .as("шлюз не должен маршрутизировать %s", path)
                    .isEqualTo(404);
        }

        productService.verify(0, getRequestedFor(urlPathMatching("/api/internal/.*")));
        userService.verify(0, getRequestedFor(urlPathMatching("/api/internal/.*")));
    }
}
