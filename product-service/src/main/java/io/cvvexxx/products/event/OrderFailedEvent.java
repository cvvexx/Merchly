package io.cvvexxx.products.event;

import java.util.List;
import java.util.UUID;

public record OrderFailedEvent(
        UUID orderId,
        List<UUID> productId,
        String reason
) {}