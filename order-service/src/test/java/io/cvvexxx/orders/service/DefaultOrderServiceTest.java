package io.cvvexxx.orders.service;

import io.cvvexxx.orders.client.ProductsRestClient;
import io.cvvexxx.orders.client.UsersRestClient;
import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.NewOrderDto;
import io.cvvexxx.orders.dto.NewOrderItemDto;
import io.cvvexxx.orders.dto.OrderDto;
import io.cvvexxx.orders.dto.ProductDto;
import io.cvvexxx.orders.entity.Order;
import io.cvvexxx.orders.entity.OrderItem;
import io.cvvexxx.orders.event.OrderCreatedEvent;
import io.cvvexxx.orders.exception.OrderNotFoundException;
import io.cvvexxx.orders.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultOrderServiceTest {

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID OTHER_USER_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    private final UUID ORDER_ID = UUID.randomUUID();

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductsRestClient productClient;

    @Mock
    private UsersRestClient usersRestClient;

    @InjectMocks
    private DefaultOrderService orderService;

    @Nested
    @DisplayName("createOrder")
    class CreateOrderTests {

        @Test
        @DisplayName("создаёт заказ, считает сумму и публикует событие в Kafka")
        void createOrder_WithValidItems_ShouldSaveOrderAndPublishEvent() {
            UUID secondProductId = UUID.randomUUID();
            NewOrderDto request = new NewOrderDto(List.of(
                    new NewOrderItemDto(PRODUCT_ID, 2),
                    new NewOrderItemDto(secondProductId, 1),
                    new NewOrderItemDto(PRODUCT_ID, 1)),
                    "address",
                    "comment"
            );

            when(productClient.findById(PRODUCT_ID)).thenReturn(product(PRODUCT_ID, new BigDecimal("100.00")));
            when(productClient.findById(secondProductId)).thenReturn(product(secondProductId, new BigDecimal("50.00")));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(ORDER_ID);
                order.setCreatedAt(Instant.parse("2026-08-12T10:00:00Z"));
                return order;
            });

            OrderDto result = orderService.createOrder(request, USER_ID);

            assertEquals(ORDER_ID, result.id());
            assertEquals(USER_ID, result.userId());
            assertEquals(OrderStatus.PENDING, result.status());
            assertEquals(new BigDecimal("350.00"), result.totalAmount());
            assertEquals(2, result.items().size());

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            verify(orderRepository).save(orderCaptor.capture());
            Order saved = orderCaptor.getValue();
            assertEquals(3, saved.getItems().stream()
                    .filter(item -> item.getProductId().equals(PRODUCT_ID))
                    .findFirst()
                    .orElseThrow()
                    .getQuantity());

            ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
            verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
            assertEquals(ORDER_ID, eventCaptor.getValue().orderId());
            assertEquals(ORDER_ID, eventCaptor.getValue().orderId());
            assertEquals(new BigDecimal("350.00"), eventCaptor.getValue().totalAmount());
        }

        @Test
        @DisplayName("если у товара нет цены, не сохраняет заказ")
        void createOrder_WhenProductPriceIsMissing_ShouldThrowAndNotSave() {
            when(productClient.findById(PRODUCT_ID)).thenReturn(product(PRODUCT_ID, null));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> orderService.createOrder(
                            new NewOrderDto(
                                    List.of(new NewOrderItemDto(PRODUCT_ID, 1)),
                                    "address",
                                    "comment"
                            ),
                            USER_ID
                    )
            );

            assertEquals("order.errors.product.price_missing", exception.getMessage());
            verify(orderRepository, never()).save(any());
            verify(applicationEventPublisher, never()).publishEvent(any());
        }
    }

    @Nested
    @DisplayName("confirmOrder")
    class ConfirmOrderTests {

        @Test
        @DisplayName("переводит CREATED заказ в PAID")
        void confirmOrder_WhenCreated_ShouldMarkPaid() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PENDING, USER_ID)));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(ORDER_ID);
                order.setCreatedAt(Instant.parse("2026-08-12T10:00:00Z"));
                return order;
            });

            OrderDto result = orderService.confirmOrder(ORDER_ID, USER_ID, false);

            assertEquals(OrderStatus.CONFIRMED, result.status());
        }

        @Test
        @DisplayName("не подтверждает чужой заказ")
        void confirmOrder_WhenNotOwner_ShouldThrowAccessDenied() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PENDING, OTHER_USER_ID)));

            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> orderService.confirmOrder(ORDER_ID, USER_ID, false)
            );

            assertEquals("order.errors.access_denied", exception.getMessage());
        }

        @Test
        @DisplayName("админ может подтвердить чужой заказ")
        void confirmOrder_WhenAdmin_ShouldConfirmForeignOrder() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PENDING, OTHER_USER_ID)));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(ORDER_ID);
                order.setCreatedAt(Instant.parse("2026-08-12T10:00:00Z"));
                return order;
            });

            OrderDto result = orderService.confirmOrder(ORDER_ID, USER_ID, true);

            assertEquals(OrderStatus.CONFIRMED, result.status());
        }

        @Test
        @DisplayName("не подтверждает уже оплаченный или отменённый заказ")
        void confirmOrder_WhenNotCreated_ShouldThrowIllegalState() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED, USER_ID)));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> orderService.confirmOrder(ORDER_ID, USER_ID, false)
            );

            assertEquals("order.errors.cannot_confirm", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("cancelOrderByUser")
    class CancelOrderTests {

        @Test
        @DisplayName("переводит CREATED заказ в CANCELLED")
        void cancelOrder_WhenCreated_ShouldMarkCancelled() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PENDING, USER_ID)));
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setId(ORDER_ID);
                order.setCreatedAt(Instant.parse("2026-08-12T10:00:00Z"));
                return order;
            });

            OrderDto result = orderService.cancelOrderByUser(ORDER_ID, USER_ID, false);

            assertEquals(OrderStatus.CANCELLED, result.status());
        }

        @Test
        @DisplayName("не отменяет оплаченный заказ")
        void cancelOrder_WhenPaid_ShouldThrowIllegalState() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED, USER_ID)));

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class,
                    () -> orderService.cancelOrderByUser(ORDER_ID, USER_ID, false)
            );

            assertEquals("order.errors.cannot_cancel", exception.getMessage());
        }

        @Test
        @DisplayName("если заказ не найден, выбрасывает NoSuchElementException")
        void cancelOrder_WhenMissing_ShouldThrowNotFound() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

            OrderNotFoundException exception = assertThrows(
                    OrderNotFoundException.class,
                    () -> orderService.cancelOrderByUser(ORDER_ID, USER_ID, false)
            );

            assertEquals("order.errors.order_not_found", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("getOrder / getCurrentUserOrders")
    class GetOrderTests {

        @Test
        @DisplayName("возвращает заказ владельца")
        void getOrder_WhenOwner_ShouldReturnOrder() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PENDING, USER_ID)));

            OrderDto result = orderService.getOrder(ORDER_ID, USER_ID, false);

            assertEquals(ORDER_ID, result.id());
            verify(orderRepository, times(1)).findById(ORDER_ID);
        }

        @Test
        @DisplayName("возвращает список заказов текущего пользователя")
        void getCurrentUserOrders_ShouldMapRepositoryResult() {
            when(orderRepository.findAllByUserIdOrderByCreatedAtDesc(USER_ID))
                    .thenReturn(List.of(order(OrderStatus.PENDING, USER_ID)));

            List<OrderDto> result = orderService.getCurrentUserOrders(USER_ID);

            assertEquals(1, result.size());
            assertEquals(ORDER_ID, result.get(0).id());
        }
    }

    @Nested
    @DisplayName("cancelOrderBySystem")
    class CancelOrderBySystemTests {

        @Test
        @DisplayName("отменяет PENDING заказ и сохраняет причину")
        void cancelOrderBySystem_WhenPending_ShouldMarkCancelledWithReason() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.PENDING, USER_ID)));

            OrderDto result = orderService.cancelOrderBySystem(ORDER_ID, "Недостаточно товара на складе");

            assertEquals(OrderStatus.CANCELLED, result.status());
            assertEquals("Недостаточно товара на складе", result.cancellationReason());
        }

        @Test
        @DisplayName("повторное событие для уже отменённого заказа игнорируется (идемпотентность)")
        void cancelOrderBySystem_WhenAlreadyCancelled_ShouldStayCancelledAndNotOverwriteReason() {
            Order cancelledOrder = order(OrderStatus.CANCELLED, USER_ID);
            cancelledOrder.setCancellationReason("Первая причина");
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(cancelledOrder));

            OrderDto result = orderService.cancelOrderBySystem(ORDER_ID, "Вторая причина");

            assertEquals(OrderStatus.CANCELLED, result.status());
            assertEquals("Первая причина", result.cancellationReason());
        }

        @Test
        @DisplayName("устаревшее событие не отменяет уже подтверждённый заказ")
        void cancelOrderBySystem_WhenAlreadyConfirmed_ShouldNotDowngradeStatus() {
            when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(order(OrderStatus.CONFIRMED, USER_ID)));

            OrderDto result = orderService.cancelOrderBySystem(ORDER_ID, "Недостаточно товара на складе");

            assertEquals(OrderStatus.CONFIRMED, result.status());
        }
    }

    private ProductDto product(UUID productId, BigDecimal price) {
        return new ProductDto(productId, 1, price);
    }

    private Order order(OrderStatus status, UUID userId) {
        Order order = Order.builder()
                .id(ORDER_ID)
                .userId(userId)
                .status(status)
                .totalAmount(new BigDecimal("100.00"))
                .createdAt(Instant.parse("2026-08-12T10:00:00Z"))
                .updatedAt(Instant.parse("2026-08-12T10:00:00Z"))
                .build();
        order.addItem(OrderItem.builder()
                .id(UUID.randomUUID())
                .productId(PRODUCT_ID)
                .price(new BigDecimal("100.00"))
                .quantity(1)
                .build());
        return order;
    }
}
