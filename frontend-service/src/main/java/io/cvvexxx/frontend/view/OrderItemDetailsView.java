package io.cvvexxx.frontend.view;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderItemDetailsView(
        UUID id,
        UUID productId,
        String title,
        BigDecimal price,
        Integer quantity
) {
}
