package io.cvvexxx.frontend.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(
        int id,
        String title,
        String description,
        BigDecimal price,
        UUID createdBy
) {
}
