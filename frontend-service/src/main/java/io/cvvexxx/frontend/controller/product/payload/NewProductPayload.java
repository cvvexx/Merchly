package io.cvvexxx.frontend.controller.product.payload;

import java.math.BigDecimal;

public record NewProductPayload(
        String title,
        String description,
        BigDecimal price,
        Integer createdBy
) {
}
