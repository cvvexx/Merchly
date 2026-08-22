package io.cvvexxx.products.kafka;

import io.cvvexxx.products.event.OrderFailedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, OrderFailedEvent> kafkaTemplate;

    @Value("${app.kafka.topics.order-failed}")
    private String orderFailedTopic;

    public void publishOrderFailed(OrderFailedEvent event) {
        log.info("Отправка OrderFailedEvent в топик {}: {}", orderFailedTopic, event);
        kafkaTemplate.send(orderFailedTopic, event.orderId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Событие отмены заказа успешно отправлено в Kafka. Offset: {}",
                                result.getRecordMetadata().offset());
                    } else {
                        log.error("Ошибка отправки события отмены заказа в Kafka", ex);
                    }
                });
    }
}