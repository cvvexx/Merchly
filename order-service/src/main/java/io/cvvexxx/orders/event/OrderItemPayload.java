package io.cvvexxx.orders.event;

import java.util.UUID;

public record OrderItemPayload(
        UUID productId,
        Integer quantity
) {
}
