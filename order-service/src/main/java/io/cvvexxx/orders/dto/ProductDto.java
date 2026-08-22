package io.cvvexxx.orders.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        int quantity,
        BigDecimal price
) {
}
