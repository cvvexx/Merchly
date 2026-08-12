package io.cvvexxx.orders.dto;

import io.cvvexxx.orders.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderDto(
        UUID id,
        UUID userId,
        OrderStatus status,
        BigDecimal totalAmount,
        String deliveryAddress,
        String comment,
        List<OrderItemDto> items,
        Instant createdAt,
        Instant updatedAt
) {}