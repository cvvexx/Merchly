package io.cvvexxx.users.service.cart;

import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.users.domain.Gender;
import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.dto.cart.CartItemDto;
import io.cvvexxx.users.entity.CartItem;
import io.cvvexxx.users.entity.User;
import io.cvvexxx.users.repository.CartItemRepository;
import io.cvvexxx.users.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.keycloak.admin.client.Keycloak;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Exercises {@link DefaultCartService} against a real Redis (Testcontainers) instead of mocking
 * the repository outright, so it proves the {@code @Cacheable}/{@code @CacheEvict} wiring on the
 * "userCart" cache actually reads from and invalidates Redis - not just that the underlying
 * business logic is correct.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(CartServiceRedisCacheIT.ITConfig.class)
class CartServiceRedisCacheIT {

    private static final String CACHE_NAME = "userCart";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");
    private final UUID userId = UUID.randomUUID();
    @Autowired
    private CartService cartService;
    @MockitoSpyBean
    private CartItemRepository cartItemRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CacheManager cacheManager;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", postgres::getDriverClassName);
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("spring.cache.type", () -> "redis");
    }

    @BeforeEach
    void createOwningUser() {
        // cart_items.user_id has a FK to users - a cart row needs an owning user to exist
        userRepository.saveAndFlush(new User(
                userId, "cache_it_" + userId, "cache_it_" + userId + "@example.com",
                Gender.F, LocalDate.of(1995, 5, 5), Set.of(), null
        ));
    }

    @AfterEach
    void cleanUp() {
        Cache cache = cacheManager.getCache(CACHE_NAME);
        if (cache != null) {
            cache.clear();
        }
        cartItemRepository.deleteAll();
        userRepository.deleteAll();
        Mockito.reset(cartItemRepository);
    }

    private CartItem persistCartItem(UUID productId, int quantity) {
        return cartItemRepository.save(CartItem.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(productId)
                .quantity(quantity)
                .build());
    }

    @TestConfiguration
    static class ITConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .claim("sub", token)
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }

        @Bean
        Keycloak keycloak() {
            return mock(Keycloak.class, Mockito.RETURNS_DEEP_STUBS);
        }
    }

    @Nested
    @DisplayName("getCartItems: чтение из Redis-кэша")
    class GetCartItemsCacheTests {

        @Test
        @DisplayName("второй вызов подряд не обращается к репозиторию - данные отдаются из Redis")
        void getCartItems_CalledTwice_ShouldHitRepositoryOnlyOnce() {
            // given
            persistCartItem(UUID.randomUUID(), 3);

            // when
            List<CartItemDto> first = cartService.getCartItems(userId);
            List<CartItemDto> second = cartService.getCartItems(userId);

            // then
            assertEquals(first, second);
            verify(cartItemRepository, times(1)).findAllByUserId(userId);
        }

        @Test
        @DisplayName("после вызова в Redis появляется запись кэша userCart для пользователя")
        void getCartItems_ShouldPopulateRedisCacheEntry() {
            // given
            persistCartItem(UUID.randomUUID(), 1);

            // when
            cartService.getCartItems(userId);

            // then
            Cache.ValueWrapper cached = cacheManager.getCache(CACHE_NAME).get(userId);
            assertNotNull(cached, "Ожидалась запись в Redis-кэше после вызова getCartItems");
        }

        @Test
        @DisplayName("если данные меняются в обход сервиса, кэш продолжает отдавать устаревшие данные из Redis")
        void getCartItems_WhenUnderlyingRowChangesWithoutEviction_ShouldServeStaleCachedData() {
            // given: прогреваем кэш
            persistCartItem(UUID.randomUUID(), 1);
            List<CartItemDto> cachedResult = cartService.getCartItems(userId);
            assertEquals(1, cachedResult.size());

            // when: меняем данные напрямую через репозиторий, минуя сервис (без @CacheEvict)
            persistCartItem(UUID.randomUUID(), 5);
            List<CartItemDto> secondResult = cartService.getCartItems(userId);

            // then: сервис всё ещё отдаёт закэшированный в Redis (устаревший) результат
            assertEquals(1, secondResult.size());
            assertEquals(cachedResult, secondResult);
        }
    }

    @Nested
    @DisplayName("Инвалидация Redis-кэша при изменении корзины")
    class CacheEvictionTests {

        @Test
        @DisplayName("addItemToCart вытесняет запись из Redis, следующий getCartItems видит новый товар")
        void addItemToCart_ShouldEvictCacheEntry() {
            // given: прогреваем кэш пустым списком
            assertTrue(cartService.getCartItems(userId).isEmpty());

            // when
            UUID productId = UUID.randomUUID();
            cartService.addItemToCart(new AddToCartDto(productId, 2), userId);

            // then: запись вытеснена из Redis сразу после изменения
            assertNull(cacheManager.getCache(CACHE_NAME).get(userId));

            // and: следующий вызов видит добавленный товар, а не устаревший пустой список
            List<CartItemDto> after = cartService.getCartItems(userId);
            assertEquals(1, after.size());
            assertEquals(productId, after.get(0).productId());
        }

        @Test
        @DisplayName("deleteritemfromcart вытесняет запись из Redis, следующий getCartItems не видит удалённый товар")
        void deleteItemFromCart_ShouldEvictCacheEntry() {
            // given
            UUID productId = UUID.randomUUID();
            persistCartItem(productId, 1);
            assertEquals(1, cartService.getCartItems(userId).size());

            // when
            cartService.deleteritemfromcart(productId, userId);

            // then
            assertNull(cacheManager.getCache(CACHE_NAME).get(userId));
            assertTrue(cartService.getCartItems(userId).isEmpty());
        }

        @Test
        @DisplayName("clearCart вытесняет запись из Redis, следующий getCartItems возвращает пустой список")
        void clearCart_ShouldEvictCacheEntry() {
            // given
            persistCartItem(UUID.randomUUID(), 1);
            persistCartItem(UUID.randomUUID(), 2);
            assertEquals(2, cartService.getCartItems(userId).size());

            // when
            cartService.clearCart(userId);

            // then
            assertNull(cacheManager.getCache(CACHE_NAME).get(userId));
            assertTrue(cartService.getCartItems(userId).isEmpty());
        }
    }
}
