package io.cvvexxx.products.kafka;

import io.cvvexxx.products.event.OrderCreatedEvent;
import io.cvvexxx.products.event.OrderFailedEvent;
import io.cvvexxx.products.exception.InsufficientStockException;
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
    private final OrderEventPublisher orderEventPublisher;

    @KafkaListener(
            topics = "${app.kafka.topics.order-created}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleOrderCreated(OrderCreatedEvent event) {
        log.info("Получено событие OrderCreatedEvent для заказа: {}", event.orderId());
        try {
            productService.deductStock(event.items());
            log.info("Успешно списаны товары для заказа: {}", event.orderId());
        } catch (InsufficientStockException e) {
            log.error("Нехватка товара {} для заказа {}", e.getProductId(), event.orderId());
            OrderFailedEvent failedEvent = new OrderFailedEvent(
                    event.orderId(),
                    e.getProductId(),
                    e.getMessage()
            );
            orderEventPublisher.publishOrderFailed(failedEvent);
        } catch (Exception e) {
            log.error("Критическая ошибка при обработке заказа: {}", event.orderId(), e);
        }
    }
}