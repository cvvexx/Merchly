package io.cvvexxx.orders.event;

import java.util.List;
import java.util.UUID;

public record OrderFailedEvent(
        UUID orderId,
        List<UUID> productIds,
        String reason
) {
}
