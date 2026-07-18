package io.cvvexxx.frontend.dto;

import java.math.BigDecimal;

public record Product(
        int id,
        String title,
        String description,
        BigDecimal price,
        Integer createdBy
) {
}
