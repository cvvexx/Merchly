package io.cvvexxx.products.event;

import java.util.UUID;

public record OrderFailedEvent(
        UUID orderId,
        UUID productId,
        String reason
) {}