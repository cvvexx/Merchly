package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderFailedEvent;
import io.cvvexxx.orders.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderFailedKafkaListener {

    private final OrderService orderService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-failed}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderFailed(OrderFailedEvent event) {
        log.warn("Получен отказ по заказу {}. Причина: {}", event.orderId(), event.reason());
        orderService.cancelOrderBySystem(event.orderId(), event.reason());
        log.info("Заказ {} успешно переведен в статус CANCELLED", event.orderId());
    }
}