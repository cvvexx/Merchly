package io.cvvexxx.products.event;

import java.util.UUID;

public record OrderItemPayload(
        UUID productId,
        Integer quantity
) {
}