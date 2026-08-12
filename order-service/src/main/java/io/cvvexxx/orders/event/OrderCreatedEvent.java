package io.cvvexxx.orders.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderCreatedEvent(
        UUID orderId,
        UUID userId,
        BigDecimal totalAmount,
        Instant createdAt,
        List<OrderCreatedItem> items
) {

    public record OrderCreatedItem(
            UUID productId,
            BigDecimal price,
            Integer quantity
    ) {
    }
}
