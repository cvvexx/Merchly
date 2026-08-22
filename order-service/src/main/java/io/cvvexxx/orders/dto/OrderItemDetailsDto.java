package io.cvvexxx.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDetailsDto(
        UUID id,
        UUID productId,
        String title,
        String imageFileName,
        BigDecimal price,
        Integer quantity
) {
}