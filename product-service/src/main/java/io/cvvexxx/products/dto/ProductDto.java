package io.cvvexxx.products.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductDto(
        UUID id,
        String title,
        String description,
        Integer quantity,
        BigDecimal price,
        String imageFileName,
        UUID createdBy
) {
}
