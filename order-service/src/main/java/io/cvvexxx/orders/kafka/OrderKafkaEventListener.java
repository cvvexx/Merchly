package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaEventListener {

    private final OrderEventPublisher orderEventPublisher;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderCreated(OrderCreatedEvent event) {
        try {
            orderEventPublisher.publishOrderCreated(event);
        } catch (Exception e) {
            log.error("Failed to publish OrderCreatedEvent for order {}", event.orderId(), e);
        }
    }
}
