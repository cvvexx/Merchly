package io.cvvexxx.orders.kafka;

import io.cvvexxx.orders.event.OrderCreatedEvent;

public interface OrderEventPublisher {

    void publishOrderCreated(OrderCreatedEvent event);

}