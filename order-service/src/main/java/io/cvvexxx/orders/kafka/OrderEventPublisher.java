package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> orderCreatedKafkaTemplate;

    @Value("${merchly.kafka.topics.order-created:order-created}")
    private String orderCreatedTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            orderCreatedKafkaTemplate
                    .send(orderCreatedTopic, event.orderId().toString(), event)
                    .get();
            log.info("Published order-created event for order {}", event.orderId());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("order.errors.event_publish_failed", exception);
        } catch (Exception exception) {
            log.error("Failed to publish order-created event for order {}", event.orderId(), exception);
            throw new IllegalStateException("order.errors.event_publish_failed", exception);
        }
    }
}
