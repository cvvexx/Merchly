package io.cvvexxx.frontend.controller.product.payload;

import java.math.BigDecimal;

public record UpdateProductPayload(
        String title,
        String description,
        BigDecimal price
) {
}
