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
import org.springframework.http.*;
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Testcontainers
class ReviewApiIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");
    @LocalServerPort
    private int port;
    @Autowired
    private ReviewRepository reviewRepository;

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

    private RestClient restClient() {
        return RestClient.builder().baseUrl("http://localhost:" + port).build();
    }

    @Test
    @DisplayName("создание, чтение (с кешем статистики), обновление и удаление отзыва через реальные Postgres+Redis")
    void reviewLifecycle_ThroughRealPostgresAndRedis_WorksEndToEnd() {
        UUID userId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID productId = UUID.randomUUID();
        NewReviewDto newReviewDto = new NewReviewDto(productId, 4, "Good product");

        ResponseEntity<ReviewDto> createResponse = restClient()
                .post()
                .uri("/api/reviews/products")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(newReviewDto)
                .retrieve()
                .toEntity(ReviewDto.class);

        assertEquals(HttpStatus.OK, createResponse.getStatusCode());
        ReviewDto created = createResponse.getBody();
        assertTrue(reviewRepository.findById(created.reviewId()).isPresent());
        assertEquals(productId, created.productId());
        assertEquals(4, created.rating());

        ResponseEntity<ReviewStatsDto> firstStats = restClient()
                .get()
                .uri("/api/reviews/products/{productId}/stats", productId)
                .retrieve()
                .toEntity(ReviewStatsDto.class);
        assertEquals(HttpStatus.OK, firstStats.getStatusCode());
        assertEquals(4.0, firstStats.getBody().averageRating());
        assertEquals(1L, firstStats.getBody().totalReviews());

        UpdateReviewDto updateReviewDto = new UpdateReviewDto(created.reviewId(), 2, "Changed my mind");
        ResponseEntity<ReviewDto> updateResponse = restClient()
                .patch()
                .uri("/api/reviews/products")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(updateReviewDto)
                .retrieve()
                .toEntity(ReviewDto.class);
        assertEquals(HttpStatus.OK, updateResponse.getStatusCode());
        assertEquals(2, updateResponse.getBody().rating());

        ResponseEntity<ReviewStatsDto> statsAfterUpdate = restClient()
                .get()
                .uri("/api/reviews/products/{productId}/stats", productId)
                .retrieve()
                .toEntity(ReviewStatsDto.class);
        assertEquals(HttpStatus.OK, statsAfterUpdate.getStatusCode());
        assertEquals(2.0, statsAfterUpdate.getBody().averageRating());

        ResponseEntity<Void> deleteResponse = restClient()
                .delete()
                .uri("/api/reviews/products/{reviewId}", created.reviewId())
                .headers(headers -> headers.addAll(authHeaders()))
                .retrieve()
                .toBodilessEntity();
        assertEquals(HttpStatus.NO_CONTENT, deleteResponse.getStatusCode());
        assertTrue(reviewRepository.findById(created.reviewId()).isEmpty());
    }

    @Test
    @DisplayName("повторное чтение статистики отзывов - второй запрос отдаётся из реального Redis-кеша без ошибок")
    void getProductStats_ReadTwiceInARow_ShouldServeSecondReadFromRedisCacheWithoutThrowing() {
        UUID productId = UUID.randomUUID();
        restClient()
                .post()
                .uri("/api/reviews/products")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(new NewReviewDto(productId, 5, "Great"))
                .retrieve()
                .toBodilessEntity();

        ResponseEntity<ReviewStatsDto> firstRead = restClient()
                .get()
                .uri("/api/reviews/products/{productId}/stats", productId)
                .retrieve()
                .toEntity(ReviewStatsDto.class);

        ResponseEntity<ReviewStatsDto> secondRead = restClient()
                .get()
                .uri("/api/reviews/products/{productId}/stats", productId)
                .retrieve()
                .toEntity(ReviewStatsDto.class);

        assertEquals(HttpStatus.OK, secondRead.getStatusCode());
        assertEquals(firstRead.getBody().productId(), secondRead.getBody().productId());
        assertEquals(firstRead.getBody().averageRating(), secondRead.getBody().averageRating());
    }

    @Test
    @DisplayName("создание отзыва с некорректным рейтингом возвращает 400 и не сохраняет ничего в Postgres")
    void createReview_WithInvalidRating_ReturnsBadRequestAndPersistsNothing() {
        UUID productId = UUID.randomUUID();
        NewReviewDto invalidReviewDto = new NewReviewDto(productId, 7, "invalid rating");
        long countBefore = reviewRepository.count();

        ResponseEntity<ProblemDetail> response = restClient()
                .post()
                .uri("/api/reviews/products")
                .headers(headers -> headers.addAll(authHeaders()))
                .body(invalidReviewDto)
                .retrieve()
                .onStatus(HttpStatusCode::isError, (req, res) -> {
                })
                .toEntity(ProblemDetail.class);

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
