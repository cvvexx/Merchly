package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderCancelledEvent;
import io.cvvexxx.orders.event.OrderCreatedEvent;

public interface OrderEventPublisher {

    void publishOrderCreated(OrderCreatedEvent event);

    void publishOrderCancelled(OrderCancelledEvent event);

}