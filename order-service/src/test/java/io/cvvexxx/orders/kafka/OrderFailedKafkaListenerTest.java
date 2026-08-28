package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderFailedEvent;
import io.cvvexxx.orders.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderFailedKafkaListenerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderFailedKafkaListener listener;

    @Test
    @DisplayName("передаёт id заказа и причину отказа в cancelOrderBySystem")
    void handleOrderFailed_ShouldCancelOrderBySystemWithReason() {
        UUID orderId = UUID.randomUUID();
        OrderFailedEvent event = new OrderFailedEvent(
                orderId, List.of(UUID.randomUUID()), "Недостаточно товара на складе"
        );

        listener.handleOrderFailed(event);

        verify(orderService).cancelOrderBySystem(orderId, "Недостаточно товара на складе");
    }

    @Test
    @DisplayName("исключение из сервиса не глотается, а пробрасывается дальше для retry/DLT")
    void handleOrderFailed_WhenServiceThrows_ShouldPropagateForKafkaRetry() {
        UUID orderId = UUID.randomUUID();
        OrderFailedEvent event = new OrderFailedEvent(
                orderId, List.of(UUID.randomUUID()), "reason"
        );
        doThrow(new RuntimeException("db is down"))
                .when(orderService).cancelOrderBySystem(orderId, "reason");

        assertThrows(RuntimeException.class, () -> listener.handleOrderFailed(event));
    }
}
