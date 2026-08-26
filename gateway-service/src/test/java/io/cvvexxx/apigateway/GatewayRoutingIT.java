package io.cvvexxx.apigateway;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayRoutingIT {

    static WireMockServer productService =
            new WireMockServer(WireMockConfiguration.options().dynamicPort().http2PlainDisabled(true));

    @LocalServerPort
    int gatewayPort;

    @BeforeAll
    static void startDownstream() {
        productService.start();
    }

    @AfterAll
    static void stopDownstream() {
        productService.stop();
    }

    @AfterEach
    void resetStubs() {
        productService.resetAll();
    }

    @DynamicPropertySource
    static void downstreamUri(DynamicPropertyRegistry registry) {
        registry.add("PRODUCT_SERVICE_URI", () -> "http://localhost:" + productService.port());
    }

    private RestClient gateway() {
        return RestClient.builder()
                .baseUrl("http://localhost:" + gatewayPort)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                })
                .build();
    }

    @Test
    @DisplayName("GET /api/products уходит в product-service, тело и статус доходят обратно без изменений")
    void getProducts_IsProxiedToProductService() {
        productService.stubFor(get(urlEqualTo("/api/products"))
                .willReturn(okJson("[{\"id\":\"1\",\"title\":\"Худи\"}]")));

        ResponseEntity<String> response = gateway().get()
                .uri("/api/products")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("Худи");
        productService.verify(getRequestedFor(urlEqualTo("/api/products")));
    }

    @Test
    @DisplayName("query-параметры доезжают до product-service")
    void queryParameters_ArePreserved() {
        productService.stubFor(get(urlPathEqualTo("/api/products"))
                .withQueryParam("filter", equalTo("худи"))
                .willReturn(okJson("[]")));

        gateway().get().uri("/api/products?filter={f}", "худи").retrieve().toBodilessEntity();

        productService.verify(getRequestedFor(urlPathEqualTo("/api/products"))
                .withQueryParam("filter", equalTo("худи")));
    }

    @Test
    @DisplayName("заголовок Authorization пробрасывается - иначе ADMIN-эндпоинты каталога недоступны через шлюз")
    void authorizationHeader_IsForwarded() {
        productService.stubFor(delete(urlPathMatching("/api/products/.*"))
                .willReturn(aResponse().withStatus(204)));

        gateway().delete()
                .uri("/api/products/{id}", "b0000000-0000-0000-0000-000000000001")
                .header("Authorization", "Bearer test-token")
                .retrieve()
                .toBodilessEntity();

        productService.verify(deleteRequestedFor(urlPathMatching("/api/products/.*"))
                .withHeader("Authorization", equalTo("Bearer test-token")));
    }

    @Test
    @DisplayName("Host остаётся адресом шлюза, поэтому Location в 201 Created указывает на шлюз, а не внутрь сети")
    void hostHeader_IsPreservedSoLocationPointsAtGateway() {
        productService.stubFor(post(urlEqualTo("/api/products"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Location", "http://localhost:" + gatewayPort + "/api/products/new-id")
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"id\":\"new-id\"}")));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("payload", new ByteArrayResource("{\"title\":\"Худи\"}".getBytes()) {
            @Override
            public String getFilename() {
                return "payload.json";
            }
        });

        ResponseEntity<Void> response = gateway().post()
                .uri("/api/products")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        productService.verify(postRequestedFor(urlEqualTo("/api/products"))
                .withHeader("Host", equalTo("localhost:" + gatewayPort)));
        assertThat(response.getHeaders().getFirst("Location"))
                .isEqualTo("http://localhost:" + gatewayPort + "/api/products/new-id");
    }

    @Test
    @DisplayName("multipart-загрузка товара проходит через шлюз без потери частей")
    void multipartUpload_IsProxiedIntact() {
        productService.stubFor(post(urlEqualTo("/api/products"))
                .willReturn(aResponse().withStatus(201)));

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("payload", new ByteArrayResource("{\"title\":\"Худи\"}".getBytes()) {
            @Override
            public String getFilename() {
                return "payload.json";
            }
        });
        body.add("image", new ByteArrayResource("fake-png-bytes".getBytes()) {
            @Override
            public String getFilename() {
                return "image.png";
            }
        });

        gateway().post()
                .uri("/api/products")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(body)
                .retrieve()
                .toBodilessEntity();

        productService.verify(postRequestedFor(urlEqualTo("/api/products"))
                .withRequestBodyPart(aMultipart().withName("payload").build())
                .withRequestBodyPart(aMultipart().withName("image").build()));
    }

    @Test
    @DisplayName("/api/internal/** наружу не публикуется: шлюз отвечает 404 и в product-service не ходит")
    void internalEndpoints_AreNotExposed() {
        ResponseEntity<String> response = gateway().get()
                .uri("/api/internal/products?ids=1")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        productService.verify(0, getRequestedFor(urlPathMatching("/api/internal/.*")));
    }

    @Test
    @DisplayName("статус ошибки downstream доходит до клиента как есть, а не подменяется шлюзом")
    void downstreamErrorStatus_IsPropagated() {
        productService.stubFor(get(urlPathMatching("/api/products/.*"))
                .willReturn(aResponse().withStatus(404).withBody("{\"error\":\"not found\"}")));

        ResponseEntity<String> response = gateway().get()
                .uri("/api/products/{id}", "missing")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(404);
        assertThat(response.getBody()).contains("not found");
    }
}
