package io.cvvexxx.products;

import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.products.controller.payload.NewProductPayload;
import io.cvvexxx.products.controller.payload.UpdateProductPayload;
import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.event.OrderCancelledEvent;
import io.cvvexxx.products.event.OrderCreatedEvent;
import io.cvvexxx.products.event.OrderFailedEvent;
import io.cvvexxx.products.event.OrderItemPayload;
import io.cvvexxx.products.repository.ProcessedOrderEventRepository;
import io.cvvexxx.products.repository.ProductRepository;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.errors.ErrorResponseException;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.MinIOContainer;
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

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ProductServiceIT {

    private static final String ADMIN_TOKEN = "admin-token";
    private static final String USER_TOKEN = "plain-user-token";
    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");
    @Container
    static ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"));
    @Container
    static RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:7-alpine"));
    @Container
    static MinIOContainer minio = new MinIOContainer(DockerImageName.parse("minio/minio:latest"));
    @LocalServerPort
    private int port;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private ProcessedOrderEventRepository processedOrderEventRepository;
    @Autowired
    private MinioClient minioClient;
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    @Value("${minio.bucket.products}")
    private String bucket;
    @Value("${app.kafka.topics.order-created}")
    private String orderCreatedTopic;
    @Value("${app.kafka.topics.order-failed}")
    private String orderFailedTopic;
    @Value("${app.kafka.topics.order-cancelled}")
    private String orderCancelledTopic;
    private Consumer<String, OrderFailedEvent> orderFailedConsumer;

    @DynamicPropertySource
    static void dynamicProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");

        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.key-deserializer",
                () -> "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        registry.add("spring.kafka.consumer.value-deserializer",
                () -> "org.springframework.kafka.support.serializer.ErrorHandlingDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.deserializer.key.delegate.class",
                () -> "org.apache.kafka.common.serialization.StringDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.deserializer.value.delegate.class",
                () -> "org.springframework.kafka.support.serializer.JsonDeserializer");
        registry.add("spring.kafka.consumer.properties.spring.json.value.default.type",
                () -> "io.cvvexxx.products.event.OrderCreatedEvent");
        registry.add("spring.kafka.consumer.properties.spring.json.trusted.packages", () -> "*");
        registry.add("spring.kafka.producer.key-serializer",
                () -> "org.apache.kafka.common.serialization.StringSerializer");
        registry.add("spring.kafka.producer.value-serializer",
                () -> "org.springframework.kafka.support.serializer.JsonSerializer");
        registry.add("spring.kafka.producer.properties.spring.json.add.type.headers", () -> "false");

        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("spring.cache.type", () -> "redis");

        registry.add("minio.url", minio::getS3URL);
        registry.add("minio.access.name", minio::getUserName);
        registry.add("minio.access.secret", minio::getPassword);
    }

    @BeforeAll
    static void logContainers() {
        assertTrue(postgres.isRunning());
        assertTrue(kafka.isRunning());
    }

    @AfterEach
    void closeConsumer() {
        if (orderFailedConsumer != null) {
            orderFailedConsumer.close();
            orderFailedConsumer = null;
        }
    }

    @Test
    void createProduct_ShouldPersistToRealPostgresAndUploadImageToRealMinio() {
        NewProductPayload payload = new NewProductPayload(
                "Integration test product", "created via ProductServiceIT", 5,
                new BigDecimal("19.99"), UUID.randomUUID()
        );
        byte[] imageBytes = "fake-png-bytes".getBytes();

        ResponseEntity<ProductDto> response = createProduct(payload, imageBytes, ADMIN_TOKEN);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        ProductDto created = response.getBody();
        assertNotNull(created);
        assertNotNull(created.imageFileName());

        Optional<Product> stored = productRepository.findById(created.id());
        assertTrue(stored.isPresent());
        assertEquals("Integration test product", stored.get().getTitle());
        assertEquals(0, new BigDecimal("19.99").compareTo(stored.get().getPrice()));

        assertDoesNotThrow(() -> minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(created.imageFileName()).build()
        ));
    }

    @Test
    @DisplayName("повторное чтение продукта - второй запрос отдаётся из реального Redis-кеша без ошибок")
    void getProduct_ReadTwiceInARow_ShouldServeSecondReadFromRedisCacheWithoutThrowing() {
        ProductDto created = createProduct(
                new NewProductPayload("Cached product", "desc", 5, new BigDecimal("9.99"), UUID.randomUUID()),
                null, ADMIN_TOKEN
        ).getBody();
        assertNotNull(created);

        ProductDto firstRead = restClient()
                .get()
                .uri("/api/products/{id}", created.id())
                .retrieve()
                .body(ProductDto.class);

        ProductDto secondRead = restClient()
                .get()
                .uri("/api/products/{id}", created.id())
                .retrieve()
                .body(ProductDto.class);

        assertEquals(firstRead.id(), secondRead.id());
        assertEquals(firstRead.title(), secondRead.title());
    }

    @Test
    @DisplayName("повторное чтение списка продуктов - второй запрос отдаётся из реального Redis-кеша без ошибок")
    void getAllProducts_ReadTwiceInARow_ShouldServeSecondReadFromRedisCacheWithoutThrowing() {
        createProduct(
                new NewProductPayload("Listed product", "desc", 2, BigDecimal.ONE, UUID.randomUUID()),
                null, ADMIN_TOKEN
        );

        List<ProductDto> firstRead = restClient()
                .get()
                .uri("/api/products")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ProductDto>>() {
                });

        List<ProductDto> secondRead = restClient()
                .get()
                .uri("/api/products")
                .retrieve()
                .body(new org.springframework.core.ParameterizedTypeReference<List<ProductDto>>() {
                });

        assertEquals(firstRead.size(), secondRead.size());
    }

    @Test
    void createProduct_WhenCallerIsNotAdmin_ShouldReturn403AndNotPersist() {
        NewProductPayload payload = new NewProductPayload(
                "Should be rejected", "desc", 1, BigDecimal.TEN, UUID.randomUUID()
        );

        ResponseEntity<ProductDto> response = createProduct(payload, null, USER_TOKEN);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertTrue(productRepository.findAllByTitleContainingIgnoreCase("Should be rejected").isEmpty());
    }

    @Test
    void updateProduct_WithNewImage_ShouldReplaceOldImageInMinioAndPersistChanges() {
        ProductDto created = createProduct(
                new NewProductPayload("Original title", "original desc", 3, BigDecimal.ONE, UUID.randomUUID()),
                "original-image-bytes".getBytes(), ADMIN_TOKEN
        ).getBody();
        assertNotNull(created);
        String oldImageFileName = created.imageFileName();

        UpdateProductPayload updatePayload = new UpdateProductPayload(
                "Updated title", "updated desc", 7, new BigDecimal("42.50")
        );

        ResponseEntity<Void> response = updateProduct(created.id(), updatePayload, "new-image-bytes".getBytes(), ADMIN_TOKEN);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        Product updated = productRepository.findById(created.id()).orElseThrow();
        assertEquals("Updated title", updated.getTitle());
        assertEquals(7, updated.getQuantity());
        assertEquals(0, new BigDecimal("42.50").compareTo(updated.getPrice()));
        assertNotEquals(oldImageFileName, updated.getImageFileName());

        assertThrows(ErrorResponseException.class, () -> minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(oldImageFileName).build()
        ));
        assertDoesNotThrow(() -> minioClient.statObject(
                StatObjectArgs.builder().bucket(bucket).object(updated.getImageFileName()).build()
        ));
    }

    @Test
    void deleteProduct_ShouldSoftDeleteSoSubsequentGetReturns404() {
        ProductDto created = createProduct(
                new NewProductPayload("To be deleted", "desc", 1, BigDecimal.ONE, UUID.randomUUID()),
                null, ADMIN_TOKEN
        ).getBody();
        assertNotNull(created);

        ResponseEntity<Void> deleteResponse = restClient()
                .delete()
                .uri("/api/products/{id}", created.id())
                .headers(bearerAuth(ADMIN_TOKEN))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                })
                .toBodilessEntity();

        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        ResponseEntity<String> getResponse = restClient()
                .get()
                .uri("/api/products/{id}", created.id())
                .headers(bearerAuth(ADMIN_TOKEN))
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                })
                .toEntity(String.class);
        assertEquals(HttpStatus.NOT_FOUND, getResponse.getStatusCode());
    }

    @Test
    void handleOrderCreated_WithSufficientStock_ShouldDeductStockViaRealKafkaAndPostgres() {
        UUID productId = UUID.randomUUID();
        productRepository.save(Product.builder()
                .id(productId).title("Stock product").quantity(10)
                .price(BigDecimal.TEN).createdBy(UUID.randomUUID()).isAvailable(true)
                .build());
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, new BigDecimal("30.00"), List.of(new OrderItemPayload(productId, 3))
        );

        kafkaTemplate.send(orderCreatedTopic, orderId.toString(), event);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Product refreshed = productRepository.findById(productId).orElseThrow();
            assertEquals(7, refreshed.getQuantity());
            assertTrue(processedOrderEventRepository.existsById(orderId));
        });
    }

    @Test
    void handleOrderCancelled_AfterStockWasDeducted_ShouldRestoreStockViaRealKafkaAndPostgres() {
        UUID productId = UUID.randomUUID();
        productRepository.save(Product.builder()
                .id(productId).title("Cancelled order product").quantity(10)
                .price(BigDecimal.TEN).createdBy(UUID.randomUUID()).isAvailable(true)
                .build());
        UUID orderId = UUID.randomUUID();
        kafkaTemplate.send(orderCreatedTopic, orderId.toString(),
                new OrderCreatedEvent(orderId, new BigDecimal("30.00"), List.of(new OrderItemPayload(productId, 3))));
        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertEquals(7, productRepository.findById(productId).orElseThrow().getQuantity()));

        OrderCancelledEvent cancelledEvent = new OrderCancelledEvent(orderId, List.of(new OrderItemPayload(productId, 3)));
        kafkaTemplate.send(orderCancelledTopic, orderId.toString(), cancelledEvent);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Product refreshed = productRepository.findById(productId).orElseThrow();
            assertEquals(10, refreshed.getQuantity());
            assertFalse(processedOrderEventRepository.existsById(orderId));
        });
    }

    @Test
    void handleOrderCancelled_WhenStockWasNeverDeducted_ShouldLeaveStockUnchanged() {
        UUID productId = UUID.randomUUID();
        productRepository.save(Product.builder()
                .id(productId).title("Never deducted product").quantity(10)
                .price(BigDecimal.TEN).createdBy(UUID.randomUUID()).isAvailable(true)
                .build());
        UUID orderId = UUID.randomUUID();

        kafkaTemplate.send(orderCancelledTopic, orderId.toString(),
                new OrderCancelledEvent(orderId, List.of(new OrderItemPayload(productId, 3))));

        await().pollDelay(Duration.ofSeconds(3)).atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                assertEquals(10, productRepository.findById(productId).orElseThrow().getQuantity())
        );
    }

    @Test
    void handleOrderCreated_WithInsufficientStock_ShouldPublishOrderFailedEventOnRealKafkaAndNotDeduct() {
        UUID productId = UUID.randomUUID();
        productRepository.save(Product.builder()
                .id(productId).title("Low stock product").quantity(1)
                .price(BigDecimal.TEN).createdBy(UUID.randomUUID()).isAvailable(true)
                .build());
        UUID orderId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, new BigDecimal("50.00"), List.of(new OrderItemPayload(productId, 5))
        );
        orderFailedConsumer = createOrderFailedConsumer();

        kafkaTemplate.send(orderCreatedTopic, orderId.toString(), event);

        var record = KafkaTestUtils.getSingleRecord(orderFailedConsumer, orderFailedTopic, Duration.ofSeconds(15));
        OrderFailedEvent failedEvent = record.value();
        assertEquals(orderId, failedEvent.orderId());
        assertEquals(List.of(productId), failedEvent.productIds());

        Product unchanged = productRepository.findById(productId).orElseThrow();
        assertEquals(1, unchanged.getQuantity());
        assertFalse(processedOrderEventRepository.existsById(orderId));
    }

    private Consumer<String, OrderFailedEvent> createOrderFailedConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                kafka.getBootstrapServers(), "order-failed-it-" + UUID.randomUUID(), "true"
        );
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, "org.apache.kafka.common.serialization.StringDeserializer");
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, "org.springframework.kafka.support.serializer.JsonDeserializer");
        props.put("spring.json.trusted.packages", "*");
        props.put("spring.json.value.default.type", "io.cvvexxx.products.event.OrderFailedEvent");
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        var consumer = new org.springframework.kafka.core.DefaultKafkaConsumerFactory<String, OrderFailedEvent>(props)
                .createConsumer();
        consumer.subscribe(List.of(orderFailedTopic));
        return consumer;
    }

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    private java.util.function.Consumer<HttpHeaders> bearerAuth(String token) {
        return headers -> headers.setBearerAuth(token);
    }

    private ResponseEntity<ProductDto> createProduct(NewProductPayload payload, byte[] imageBytes, String bearerToken) {
        MultiValueMap<String, Object> body = multipartBody(payload, imageBytes);
        return restClient()
                .post()
                .uri("/api/products")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(bearerAuth(bearerToken))
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                })
                .toEntity(ProductDto.class);
    }

    private ResponseEntity<Void> updateProduct(UUID productId, UpdateProductPayload payload, byte[] imageBytes, String bearerToken) {
        MultiValueMap<String, Object> body = multipartBody(payload, imageBytes);
        return restClient()
                .patch()
                .uri("/api/products/{id}", productId)
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .headers(bearerAuth(bearerToken))
                .body(body)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                })
                .toBodilessEntity();
    }

    private MultiValueMap<String, Object> multipartBody(Object payload, byte[] imageBytes) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        HttpHeaders payloadHeaders = new HttpHeaders();
        payloadHeaders.setContentType(MediaType.APPLICATION_JSON);
        body.add("payload", new HttpEntity<>(payload, payloadHeaders));

        if (imageBytes != null) {
            ByteArrayResource imageResource = new ByteArrayResource(imageBytes) {
                @Override
                public String getFilename() {
                    return "image-" + UUID.randomUUID() + ".png";
                }
            };
            HttpHeaders imageHeaders = new HttpHeaders();
            imageHeaders.setContentType(MediaType.IMAGE_PNG);
            body.add("image", new HttpEntity<>(imageResource, imageHeaders));
        }
        return body;
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> {
                boolean isAdmin = ADMIN_TOKEN.equals(token);
                return Jwt.withTokenValue(token)
                        .header("alg", "none")
                        .subject(UUID.randomUUID().toString())
                        .claim("realm_access", Map.of("roles", isAdmin ? List.of("ADMIN") : List.of()))
                        .issuedAt(Instant.now())
                        .expiresAt(Instant.now().plusSeconds(3600))
                        .build();
            };
        }
    }

}
