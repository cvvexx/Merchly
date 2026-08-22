package io.cvvexxx.frontend.view;

import io.cvvexxx.frontend.dto.order.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDetailsView(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String deliveryAddress,
        String comment,
        String cancellationReason,
        List<OrderItemDetailsView> items,
        Instant createdAt,
        Instant updatedAt
) {
}
