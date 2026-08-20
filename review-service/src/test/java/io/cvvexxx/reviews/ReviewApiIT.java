package io.cvvexxx.reviews;

import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.dto.ReviewStatsDto;
import io.cvvexxx.reviews.dto.UpdateReviewDto;
import io.cvvexxx.reviews.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Real end-to-end test: boots the full Spring context against a real
 * Postgres and a real Redis (both Testcontainers, both on dynamically
 * assigned ports/addresses - production wires different addresses via
 * env vars, so nothing here is hardcoded), drives it through actual HTTP
 * calls, and verifies persistence/caching behaviour for real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ReviewApiIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");

        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("spring.data.redis.password", () -> "");
        registry.add("spring.cache.type", () -> "redis");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ReviewRepository reviewRepository;

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @DisplayName("создание, чтение (с кешем статистики), обновление и удаление отзыва через реальные Postgres+Redis")
    void reviewLifecycle_ThroughRealPostgresAndRedis_WorksEndToEnd() {
        // given
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID productId = UUID.randomUUID();
        NewReviewDto newReviewDto = new NewReviewDto(productId, 4, "Good product");

        // when: создаём отзыв
        ResponseEntity<ReviewDto> createResponse = restClient()
                .post()
                .uri("/api/reviews/products")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(newReviewDto)
                .retrieve()
                .toEntity(ReviewDto.class);

        // then: отзыв реально сохранён в Postgres
        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        ReviewDto created = createResponse.getBody();
        assertTrue(reviewRepository.findById(created.reviewId()).isPresent());
        assertEquals(productId, created.productId());
        assertEquals(4, created.rating());

        // when: читаем статистику по продукту первый раз (наполняем кеш в Redis)
        ResponseEntity<ReviewStatsDto> firstStats = restClient()
                .get()
                .uri("/api/reviews/products/{productId}/stats", productId)
                .retrieve()
                .toEntity(ReviewStatsDto.class);
        // then
        assertEquals(HttpStatus.OK, firstStats.getStatusCode());
        assertEquals(4.0, firstStats.getBody().averageRating());
        assertEquals(1L, firstStats.getBody().totalReviews());

        // when: обновляем отзыв (должен инвалидировать кеш статистики в Redis)
        UpdateReviewDto updateReviewDto = new UpdateReviewDto(created.reviewId(), 2, "Changed my mind");
        ResponseEntity<ReviewDto> updateResponse = restClient()
                .patch()
                .uri("/api/reviews/products")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(updateReviewDto)
                .retrieve()
                .toEntity(ReviewDto.class);
        // then
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(2, updateResponse.getBody().rating());

        // when: читаем статистику снова - должна отражать обновлённый рейтинг, а не устаревшее закешированное значение
        ResponseEntity<ReviewStatsDto> statsAfterUpdate = restClient()
                .get()
                .uri("/api/reviews/products/{productId}/stats", productId)
                .retrieve()
                .toEntity(ReviewStatsDto.class);
        // then
        assertEquals(HttpStatus.OK, statsAfterUpdate.getStatusCode());
        assertEquals(2.0, statsAfterUpdate.getBody().averageRating());

        // when: удаляем отзыв
        ResponseEntity<Void> deleteResponse = restClient()
                .delete()
                .uri("/api/reviews/products/{reviewId}", created.reviewId())
                .headers(headers -> headers.addAll(authHeaders()))
                .retrieve()
                .toBodilessEntity();
        // then: отзыв реально удалён из Postgres
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        assertTrue(reviewRepository.findById(created.reviewId()).isEmpty());
    }

    @Test
    @DisplayName("создание отзыва с некорректным рейтингом возвращает 400 и не сохраняет ничего в Postgres")
    void createReview_WithInvalidRating_ReturnsBadRequestAndPersistsNothing() {
        // given
        UUID productId = UUID.randomUUID();
        NewReviewDto invalidReviewDto = new NewReviewDto(productId, 7, "invalid rating");
        long countBefore = reviewRepository.count();

        // when
        ResponseEntity<ProblemDetail> response = restClient()
                .post()
                .uri("/api/reviews/products")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(invalidReviewDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> { })
                .toEntity(ProblemDetail.class);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(countBefore, reviewRepository.count());
    }

    private HttpHeaders authHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth("integration-test-token");
        return headers;
    }

    @TestConfiguration
    static class JwtTestConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject("00000000-0000-0000-0000-000000000001")
                    .claim("realm_access", java.util.Map.of("roles", List.of("ADMIN")))
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }
}
