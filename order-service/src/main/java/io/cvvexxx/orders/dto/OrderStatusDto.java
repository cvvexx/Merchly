package io.cvvexxx.orders.dto;

import io.cvvexxx.orders.domain.OrderStatus;

import java.util.UUID;

public record OrderStatusDto(
        UUID orderId,
        OrderStatus status,
        String cancellationReason
) {
}
