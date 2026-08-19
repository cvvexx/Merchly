package io.cvvexxx.products.kafka;

import io.cvvexxx.products.event.OrderCreatedEvent;
import io.cvvexxx.products.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderKafkaListener {

    private final ProductService productService;

    @KafkaListener(
            topics = "${app.kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Получено событие OrderCreatedEvent для заказа: {}", event.orderId());
        try {
            productService.deductStock(event.items());
            log.info("Успешно списаны товары для заказа: {}", event.orderId());
        } catch (Exception e) {
            log.error("Ошибка при списании товаров для заказа: {}", event.orderId(), e);
            //TODO(ДОБАВИТЬ ОТПРАВКУ СООБЩЕИЯ ОБРАТНО В ORDER СЕРВИС)
        }
    }
}