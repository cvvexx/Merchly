package io.cvvexxx.apigateway;

import io.cvvexxx.apigateway.support.RedisBackedGatewayIT;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.ServerSocket;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class GatewayFallbackIT extends RedisBackedGatewayIT {

    @LocalServerPort
    int gatewayPort;

    @DynamicPropertySource
    static void unreachableDownstream(DynamicPropertyRegistry registry) {
        registry.add("PRODUCT_SERVICE_URI", () -> "http://localhost:" + findClosedPort());
    }

    private static int findClosedPort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            throw new IllegalStateException("не удалось подобрать свободный порт", e);
        }
    }

    @Test
    @DisplayName("при недоступном product-service шлюз отдаёт 503 и тело fallback-а")
    void unavailableProductService_YieldsFallbackResponse() {
        ResponseEntity<String> response = RestClient.builder()
                .baseUrl("http://localhost:" + gatewayPort)
                .defaultStatusHandler(HttpStatusCode::isError, (req, res) -> {
                })
                .build()
                .get()
                .uri("/api/products")
                .retrieve()
                .toEntity(String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(503);
        assertThat(response.getBody()).contains("product_service_unavailable");
    }
}
