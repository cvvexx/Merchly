package io.cvvexxx.orders;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.NewOrderDto;
import io.cvvexxx.orders.dto.NewOrderItemDto;
import io.cvvexxx.orders.dto.OrderDto;
import io.cvvexxx.orders.entity.Order;
import io.cvvexxx.orders.event.OrderCancelledEvent;
import io.cvvexxx.orders.repository.OrderRepository;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static com.github.tomakehurst.wiremock.client.WireMock.delete;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.deleteRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.noContent;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Full-stack integration test: real Postgres and Kafka via Testcontainers, downstream
 * product-service/user-service calls stubbed with WireMock. No address (DB, broker, downstream
 * service) is hardcoded - everything is discovered at runtime via {@link DynamicPropertySource},
 * the same way production wires different addresses per environment via env vars.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class OrderApiIT {

    private static final WireMockServer wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());

    static {
        wireMockServer.start();
        wireMockServer.stubFor(post(urlEqualTo("/oauth2/token"))
                .willReturn(okJson("""
                        {"access_token":"test-token","token_type":"bearer","expires_in":3600}
                        """)));
        wireMockServer.stubFor(delete(urlEqualTo("/api/users/cart"))
                .willReturn(noContent()));
        // Запасной ответ для незастабленных товаров: настоящий product-service на неизвестные
        // id отдаёт пустой список, а не 404
        wireMockServer.stubFor(get(urlPathEqualTo("/api/internal/products"))
                .atPriority(10)
                .willReturn(okJson("[]")));
    }

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));

    // Кеш заказов работает через Redis: без своего контейнера тест ушёл бы на localhost:6379
    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("spring.cache.type", () -> "redis");
        registry.add("spring.restclient.uri.product_service", wireMockServer::baseUrl);
        registry.add("spring.restclient.uri.user_service", wireMockServer::baseUrl);
        registry.add("spring.security.oauth2.client.provider.internal-service-client.token-uri",
                () -> wireMockServer.baseUrl() + "/oauth2/token");
    }

    @AfterAll
    static void stopWireMock() {
        wireMockServer.stop();
    }

    @LocalServerPort
    private int port;

    @Autowired
    private OrderRepository orderRepository;

    @Value("${app.kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void createOrder_WhenProductServiceRespondsAndUserIsAuthenticated_ShouldPersistOrderAndClearCart() {
        // given
        UUID productId = UUID.randomUUID();
        stubProduct(productId, new BigDecimal("49.90"));
        NewOrderDto newOrderDto = new NewOrderDto(
                List.of(new NewOrderItemDto(productId, 2)),
                "Moscow, Lenina 1",
                "leave at the door"
        );

        // when
        ResponseEntity<OrderDto> response = restClient()
                .post()
                .uri("/api/orders/create")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(newOrderDto)
                .retrieve()
                .toEntity(OrderDto.class);

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertNotNull(response.getHeaders().getLocation());
        OrderDto createdOrder = response.getBody();
        assertNotNull(createdOrder);
        assertEquals(0, new BigDecimal("99.80").compareTo(createdOrder.totalAmount()));
        assertEquals(OrderStatus.PENDING, createdOrder.status());

        Optional<Order> persisted = orderRepository.findById(createdOrder.id());
        assertTrue(persisted.isPresent());
        assertEquals("Moscow, Lenina 1", persisted.get().getDeliveryAddress());

        wireMockServer.verify(deleteRequestedFor(urlEqualTo("/api/users/cart")));
    }

    @Test
    void createOrder_WhenProductServiceHasNoSuchProduct_ShouldFailAndNotPersistOrder() {
        // given
        UUID unknownProductId = UUID.randomUUID(); // deliberately not stubbed -> product-service replies with an empty list
        NewOrderDto newOrderDto = new NewOrderDto(
                List.of(new NewOrderItemDto(unknownProductId, 1)),
                "Moscow, Lenina 1",
                null
        );
        long ordersBefore = orderRepository.count();

        // when
        ResponseEntity<String> response = restClient()
                .post()
                .uri("/api/orders/create")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(newOrderDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { })
                .toEntity(String.class);

        // then
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(ordersBefore, orderRepository.count());
    }

    @Test
    void cancelOrder_WhenPending_ShouldPublishOrderCancelledEventToRealKafkaSoProductServiceCanRestoreStock() {
        // given: заказ создан и уже мог быть обработан product-service (сток списан)
        UUID productId = UUID.randomUUID();
        stubProduct(productId, new BigDecimal("49.90"));
        NewOrderDto newOrderDto = new NewOrderDto(
                List.of(new NewOrderItemDto(productId, 2)),
                "Moscow, Lenina 1",
                null
        );
        OrderDto createdOrder = restClient()
                .post()
                .uri("/api/orders/create")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(newOrderDto)
                .retrieve()
                .body(OrderDto.class);

        var orderCancelledConsumer = createOrderCancelledConsumer();
        try {
            // when
            ResponseEntity<OrderDto> cancelResponse = restClient()
                    .post()
                    .uri("/api/orders/{id}/cancel", createdOrder.id())
                    .headers(headers -> headers.addAll(authHeaders()))
                    .retrieve()
                    .toEntity(OrderDto.class);

            // then: заказ отменён в БД...
            assertEquals(HttpStatus.OK, cancelResponse.getStatusCode());
            assertEquals(OrderStatus.CANCELLED, orderRepository.findById(createdOrder.id()).orElseThrow().getStatus());

            // ...и в реальный топик order-cancelled опубликовано компенсирующее событие с товарами заказа,
            // чтобы product-service вернул списанный сток обратно на склад
            var record = KafkaTestUtils.getSingleRecord(orderCancelledConsumer, orderCancelledTopic, Duration.ofSeconds(15));
            assertEquals(createdOrder.id(), record.value().orderId());
            assertEquals(1, record.value().items().size());
            assertEquals(productId, record.value().items().get(0).productId());
            assertEquals(2, record.value().items().get(0).quantity());
        } finally {
            orderCancelledConsumer.close();
        }
    }

    private Consumer<String, OrderCancelledEvent> createOrderCancelledConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(), "order-cancelled-it-" + UUID.randomUUID(), "true"
        );
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonDeserializer");
        props.put("spring.json.trusted.packages", "*");
        props.put("spring.json.value.default.type", "io.cvvexxx.orders.event.OrderCancelledEvent");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var consumer = new DefaultKafkaConsumerFactory<String, OrderCancelledEvent>(props).createConsumer();
        consumer.subscribe(List.of(orderCancelledTopic));
        return consumer;
    }

    /**
     * order-service забирает товары заказа одним батчевым запросом
     * {@code GET /api/internal/products?ids=...}, поэтому стаб отвечает списком.
     */
    private void stubProduct(UUID productId, BigDecimal price) {
        wireMockServer.stubFor(get(urlPathEqualTo("/api/internal/products"))
                .withQueryParam("ids", equalTo(productId.toString()))
                .willReturn(okJson("""
                        [{"id":"%s","quantity":1,"price":%s}]
                        """.formatted(productId, price))));
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, "Bearer test-user-token");
        return headers;
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("00000000-0000-0000-0000-000000000001")
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }
}
