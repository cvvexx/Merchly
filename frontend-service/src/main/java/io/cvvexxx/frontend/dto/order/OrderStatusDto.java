package io.cvvexxx.frontend.dto.order;

import java.util.UUID;

public record OrderStatusDto(
        UUID orderId,
        OrderStatus status
) {
}
