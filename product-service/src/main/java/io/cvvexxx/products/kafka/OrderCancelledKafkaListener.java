package io.cvvexxx.products.kafka;

import io.cvvexxx.products.event.OrderCancelledEvent;
import io.cvvexxx.products.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCancelledKafkaListener {

    private final ProductService productService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-cancelled}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderCancelledKafkaListenerContainerFactory"
    )
    public void handleOrderCancelled(OrderCancelledEvent event) {
        log.info("Получено событие OrderCancelledEvent для заказа: {}", event.orderId());
        productService.restoreStock(event.orderId(), event.items());
        log.info("Остаток товара по заказу {} восстановлен", event.orderId());
    }
}
