package io.cvvexxx.products.event;

import java.util.List;
import java.util.UUID;

public record OrderCancelledEvent(
        UUID orderId,
        List<OrderItemPayload> items
) {
}
