package io.cvvexxx.frontend.controller.product.payload;

import java.math.BigDecimal;
import java.util.UUID;

public record NewProductPayload(
        String title,
        String description,
        BigDecimal price,
        UUID createdBy
) {
}
