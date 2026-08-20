package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderCreatedEvent;
import io.cvvexxx.orders.event.OrderItemPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderKafkaEventListenerTest {
    @Mock
    private OrderEventPublisher orderEventPublisher;
    @InjectMocks
    private OrderKafkaEventListener listener;

    @Test
    void onOrderCreated_ShouldPublishToKafka() {
        // given
        OrderCreatedEvent event = new OrderCreatedEvent(UUID.randomUUID(), new BigDecimal("100.00"), List.of(
                new OrderItemPayload(UUID.randomUUID(), 1)
        ));

        // when
        listener.onOrderCreated(event);

        // then
        verify(orderEventPublisher).publishOrderCreated(event);
    }
}