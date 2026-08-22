package io.cvvexxx.products.kafka;

import io.cvvexxx.products.event.OrderCreatedEvent;
import io.cvvexxx.products.event.OrderFailedEvent;
import io.cvvexxx.products.event.OrderItemPayload;
import io.cvvexxx.products.exception.InsufficientStockException;
import io.cvvexxx.products.service.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderKafkaListenerTest {

    @Mock
    private ProductService productService;

    @Mock
    private OrderEventPublisher orderEventPublisher;

    @InjectMocks
    private OrderKafkaListener listener;

    @Test
    @DisplayName("при достаточном остатке списывает товар и не публикует OrderFailedEvent")
    void handleOrderCreated_WithSufficientStock_ShouldDeductStockAndNotPublishFailure() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, new BigDecimal("100.00"), List.of(new OrderItemPayload(productId, 2))
        );

        // when
        listener.handleOrderCreated(event);

        // then
        verify(productService).deductStock(orderId, event.items());
        verifyNoInteractions(orderEventPublisher);
    }

    @Test
    @DisplayName("при нехватке товара публикует OrderFailedEvent с причиной и id заказа, а не пробрасывает исключение дальше")
    void handleOrderCreated_WithInsufficientStock_ShouldPublishOrderFailedEvent() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, new BigDecimal("100.00"), List.of(new OrderItemPayload(productId, 5))
        );

        InsufficientStockException exception = new InsufficientStockException(
                List.of(productId), "Недостаточно товара на складе ID: [" + productId + "]"
        );
        doThrow(exception).when(productService).deductStock(orderId, event.items());

        // when
        listener.handleOrderCreated(event);

        // then
        ArgumentCaptor<OrderFailedEvent> captor = ArgumentCaptor.forClass(OrderFailedEvent.class);
        verify(orderEventPublisher).publishOrderFailed(captor.capture());
        assertEquals(orderId, captor.getValue().orderId());
        assertEquals(List.of(productId), captor.getValue().productIds());
        assertEquals(exception.getMessage(), captor.getValue().reason());
    }

    @Test
    @DisplayName("непредвиденное исключение (например обрыв связи с БД) не глотается, а пробрасывается дальше для retry/DLT")
    void handleOrderCreated_WithUnexpectedException_ShouldPropagateForKafkaRetry() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, new BigDecimal("100.00"), List.of(new OrderItemPayload(productId, 1))
        );

        doThrow(new RuntimeException("db is down")).when(productService).deductStock(orderId, event.items());

        // when / then
        assertThrows(RuntimeException.class, () -> listener.handleOrderCreated(event));

        verifyNoInteractions(orderEventPublisher);
    }
}
