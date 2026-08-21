package io.cvvexxx.orders.service;

import com.redis.testcontainers.RedisContainer;
import io.cvvexxx.orders.client.ProductsRestClient;
import io.cvvexxx.orders.client.UsersRestClient;
import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.NewOrderDto;
import io.cvvexxx.orders.dto.NewOrderItemDto;
import io.cvvexxx.orders.dto.OrderDto;
import io.cvvexxx.orders.dto.ProductDto;
import io.cvvexxx.orders.entity.Order;
import io.cvvexxx.orders.entity.OrderItem;
import io.cvvexxx.orders.kafka.OrderEventPublisher;
import io.cvvexxx.orders.repository.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Exercises {@link DefaultOrderService} against a real Redis (Testcontainers) instead of mocking
 * the repository outright, so it proves the {@code @Cacheable}/{@code @CacheEvict} wiring on the
 * "order"/"userOrder" caches actually reads from and invalidates Redis - not just that the
 * underlying business logic is correct. Downstream collaborators (product-service, user-service,
 * Kafka publishing) are mocked - they are unrelated to caching and are already covered by
 * {@link io.cvvexxx.orders.OrderApiIT}.
 */
@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Import(OrderServiceRedisCacheIT.ITConfig.class)
class OrderServiceRedisCacheIT {

    private static final String ORDER_CACHE = "order";
    private static final String USER_ORDER_CACHE = "userOrder";

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static RedisContainer redis = new RedisContainer("redis:7-alpine");

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");

        registry.add("spring.data.redis.host", redis::getRedisHost);
        registry.add("spring.data.redis.port", redis::getRedisPort);
        registry.add("spring.cache.type", () -> "redis");
    }

    @TestConfiguration
    static class ITConfig {

        @Bean
        JwtDecoder jwtDecoder() {
            return token -> Jwt.withTokenValue(token)
                    .header("alg", "none")
                    .subject(UUID.randomUUID().toString())
                    .issuedAt(Instant.now())
                    .expiresAt(Instant.now().plusSeconds(3600))
                    .build();
        }
    }

    @Autowired
    private OrderService orderService;

    @MockitoSpyBean
    private OrderRepository orderRepository;

    @Autowired
    private CacheManager cacheManager;

    @MockitoBean
    private ProductsRestClient productsRestClient;

    @MockitoBean
    private UsersRestClient usersRestClient;

    @MockitoBean
    private OrderEventPublisher orderEventPublisher;

    private final UUID userId = UUID.randomUUID();
    private final UUID productId = UUID.randomUUID();

    @AfterEach
    void cleanUp() {
        cacheManager.getCacheNames().forEach(name -> {
            Cache cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
        orderRepository.deleteAll();
        Mockito.reset(orderRepository);
    }

    private Order persistOrder(OrderStatus status) {
        Order order = Order.builder()
                .userId(userId)
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .deliveryAddress("Moscow, Lenina 1")
                .build();
        order.addItem(OrderItem.builder()
                .productId(productId)
                .price(new BigDecimal("100.00"))
                .quantity(1)
                .build());
        return orderRepository.saveAndFlush(order);
    }

    @Nested
    @DisplayName("getOrder: чтение из Redis-кэша \"order\"")
    class GetOrderCacheTests {

        @Test
        @DisplayName("второй вызов подряд не обращается к репозиторию - заказ отдаётся из Redis")
        void getOrder_CalledTwice_ShouldHitRepositoryOnlyOnce() {
            // given
            Order order = persistOrder(OrderStatus.PENDING);

            // when
            OrderDto first = orderService.getOrder(order.getId(), userId, false);
            OrderDto second = orderService.getOrder(order.getId(), userId, false);

            // then: сравниваем по полям, а не через equals() - BigDecimal, пройдя через
            // Redis JSON (де)сериализацию, может сменить scale и перестать быть equals-равным
            // при том, что compareTo/значение остаются теми же
            assertEquals(first.id(), second.id());
            assertEquals(first.status(), second.status());
            assertEquals(0, first.totalAmount().compareTo(second.totalAmount()));
            verify(orderRepository, times(1)).findById(order.getId());
        }

        @Test
        @DisplayName("после вызова в Redis появляется запись кэша order::<orderId>")
        void getOrder_ShouldPopulateRedisCacheEntry() {
            // given
            Order order = persistOrder(OrderStatus.PENDING);

            // when
            orderService.getOrder(order.getId(), userId, false);

            // then
            Cache.ValueWrapper cached = cacheManager.getCache(ORDER_CACHE).get(order.getId());
            assertNotNull(cached, "Ожидалась запись в Redis-кэше после вызова getOrder");
        }

        @Test
        @DisplayName("если статус заказа меняется в обход сервиса, кэш продолжает отдавать устаревшие данные из Redis")
        void getOrder_WhenUnderlyingRowChangesWithoutEviction_ShouldServeStaleCachedData() {
            // given: прогреваем кэш
            Order order = persistOrder(OrderStatus.PENDING);
            OrderDto cached = orderService.getOrder(order.getId(), userId, false);
            assertEquals(OrderStatus.PENDING, cached.status());

            // when: меняем статус напрямую через репозиторий, минуя сервис (без @CacheEvict)
            order.setStatus(OrderStatus.CONFIRMED);
            orderRepository.saveAndFlush(order);
            OrderDto secondRead = orderService.getOrder(order.getId(), userId, false);

            // then: сервис всё ещё отдаёт закэшированный в Redis (устаревший) статус
            assertEquals(OrderStatus.PENDING, secondRead.status());
        }
    }

    @Nested
    @DisplayName("getCurrentUserOrders: чтение из Redis-кэша \"userOrder\"")
    class GetCurrentUserOrdersCacheTests {

        @Test
        @DisplayName("второй вызов подряд не обращается к репозиторию - список отдаётся из Redis")
        void getCurrentUserOrders_CalledTwice_ShouldHitRepositoryOnlyOnce() {
            // given
            persistOrder(OrderStatus.PENDING);

            // when
            List<OrderDto> first = orderService.getCurrentUserOrders(userId);
            List<OrderDto> second = orderService.getCurrentUserOrders(userId);

            // then
            assertEquals(1, first.size());
            assertEquals(first.get(0).id(), second.get(0).id());
            verify(orderRepository, times(1)).findAllByUserIdOrderByCreatedAtDesc(userId);
        }

        @Test
        @DisplayName("новый заказ, созданный в обход сервиса, не появляется во втором вызове - список читается из Redis")
        void getCurrentUserOrders_WhenNewOrderAddedWithoutEviction_ShouldServeStaleCachedList() {
            // given: прогреваем кэш одним заказом
            persistOrder(OrderStatus.PENDING);
            List<OrderDto> cached = orderService.getCurrentUserOrders(userId);
            assertEquals(1, cached.size());

            // when: добавляем ещё один заказ напрямую через репозиторий, минуя сервис
            persistOrder(OrderStatus.PENDING);
            List<OrderDto> secondRead = orderService.getCurrentUserOrders(userId);

            // then
            assertEquals(1, secondRead.size());
        }
    }

    @Nested
    @DisplayName("Инвалидация Redis-кэша при изменении заказа")
    class CacheEvictionTests {

        @Test
        @DisplayName("confirmOrder вытесняет заказ и список заказов пользователя из Redis")
        void confirmOrder_ShouldEvictOrderAndUserOrdersCaches() {
            // given: прогреваем оба кэша
            Order order = persistOrder(OrderStatus.PENDING);
            orderService.getOrder(order.getId(), userId, false);
            orderService.getCurrentUserOrders(userId);

            // when
            OrderDto confirmed = orderService.confirmOrder(order.getId(), userId, false);

            // then: обе записи вытеснены из Redis сразу после изменения
            assertEquals(OrderStatus.CONFIRMED, confirmed.status());
            assertNull(cacheManager.getCache(ORDER_CACHE).get(order.getId()));
            assertNull(cacheManager.getCache(USER_ORDER_CACHE).get(userId));

            // and: следующие вызовы видят актуальный статус, а не устаревший из старого кэша
            assertEquals(OrderStatus.CONFIRMED, orderService.getOrder(order.getId(), userId, false).status());
            assertEquals(OrderStatus.CONFIRMED, orderService.getCurrentUserOrders(userId).get(0).status());
        }

        @Test
        @DisplayName("cancelOrderByUser вытесняет заказ и список заказов пользователя из Redis")
        void cancelOrderByUser_ShouldEvictOrderAndUserOrdersCaches() {
            // given
            Order order = persistOrder(OrderStatus.PENDING);
            orderService.getOrder(order.getId(), userId, false);
            orderService.getCurrentUserOrders(userId);

            // when
            OrderDto cancelled = orderService.cancelOrderByUser(order.getId(), userId, false);

            // then
            assertEquals(OrderStatus.CANCELLED, cancelled.status());
            assertNull(cacheManager.getCache(ORDER_CACHE).get(order.getId()));
            assertNull(cacheManager.getCache(USER_ORDER_CACHE).get(userId));
            assertEquals(OrderStatus.CANCELLED, orderService.getOrder(order.getId(), userId, false).status());
        }

        @Test
        @DisplayName("cancelOrderBySystem вытесняет заказ и список заказов пользователя из Redis")
        void cancelOrderBySystem_ShouldEvictOrderAndUserOrdersCaches() {
            // given
            Order order = persistOrder(OrderStatus.PENDING);
            orderService.getOrder(order.getId(), userId, false);
            orderService.getCurrentUserOrders(userId);

            // when
            OrderDto cancelled = orderService.cancelOrderBySystem(order.getId(), "Недостаточно товара на складе");

            // then
            assertEquals(OrderStatus.CANCELLED, cancelled.status());
            assertNull(cacheManager.getCache(ORDER_CACHE).get(order.getId()));
            assertNull(cacheManager.getCache(USER_ORDER_CACHE).get(userId));
        }

        @Test
        @DisplayName("createOrder вытесняет список заказов пользователя из Redis")
        void createOrder_ShouldEvictUserOrdersCache() {
            // given: прогреваем кэш списком из одного ранее оформленного заказа
            persistOrder(OrderStatus.PENDING);
            List<OrderDto> cached = orderService.getCurrentUserOrders(userId);
            assertEquals(1, cached.size());

            when(productsRestClient.findAllProductsByIds(anyList()))
                    .thenReturn(List.of(new ProductDto(productId, 10, new BigDecimal("49.90"))));

            NewOrderDto newOrderDto = new NewOrderDto(
                    List.of(new NewOrderItemDto(productId, 2)),
                    "Moscow, Lenina 1",
                    "leave at the door"
            );

            // when
            orderService.createOrder(newOrderDto, userId);

            // then: запись вытеснена из Redis сразу после создания заказа
            assertNull(cacheManager.getCache(USER_ORDER_CACHE).get(userId));

            // and: следующий вызов видит оба заказа, а не устаревший список из одного
            assertEquals(2, orderService.getCurrentUserOrders(userId).size());
        }
    }
}
