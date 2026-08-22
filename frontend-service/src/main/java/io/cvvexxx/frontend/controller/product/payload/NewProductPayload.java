package io.cvvexxx.frontend.controller.product.payload;

import java.math.BigDecimal;
import java.util.UUID;

public record NewProductPayload(
        String title,
        String description,
        Integer quantity,
        BigDecimal price,
        UUID createdBy
) {
}
