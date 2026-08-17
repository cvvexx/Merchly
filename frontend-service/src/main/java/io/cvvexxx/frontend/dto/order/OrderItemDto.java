package io.cvvexxx.frontend.dto.order;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDto(
        UUID id,
        UUID productId,
        BigDecimal price,
        Integer quantity
) {
}
