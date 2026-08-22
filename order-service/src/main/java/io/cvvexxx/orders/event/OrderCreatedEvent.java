package io.cvvexxx.orders.event;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        BigDecimal totalAmount,
        List<OrderItemPayload> items
) {
}