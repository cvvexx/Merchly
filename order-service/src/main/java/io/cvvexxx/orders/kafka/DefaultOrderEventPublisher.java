package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderCancelledEvent;
import io.cvvexxx.orders.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DefaultOrderEventPublisher implements OrderEventPublisher {

    private final KafkaTemplate<String, OrderCreatedEvent> kafkaTemplate;
    private final KafkaTemplate<String, OrderCancelledEvent> orderCancelledKafkaTemplate;

    @Value("${app.kafka.topics.order-created}")
    private String orderCreatedTopic;

    @Value("${app.kafka.topics.order-cancelled}")
    private String orderCancelledTopic;

    public void publishOrderCreated(OrderCreatedEvent event) {
        log.info("Отправка события OrderCreatedEvent в топик {}: {}", orderCreatedTopic, event);
        kafkaTemplate.send(orderCreatedTopic, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Событие успешно отправлено. Offset: {}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка при отправке события в Kafka", ex);
                    }
                });
    }

    public void publishOrderCancelled(OrderCancelledEvent event) {
        log.info("Отправка события OrderCancelledEvent в топик {}: {}", orderCancelledTopic, event);
        orderCancelledKafkaTemplate.send(orderCancelledTopic, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Событие отмены заказа успешно отправлено. Offset: {}", result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка при отправке события отмены заказа в Kafka", ex);
                    }
                });
    }
}
