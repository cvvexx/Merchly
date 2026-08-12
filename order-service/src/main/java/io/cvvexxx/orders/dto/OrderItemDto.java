package io.cvvexxx.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID productId,
        BigDecimal price,
        Integer quantity
) {}