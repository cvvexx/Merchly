package io.cvvexxx.frontend.dto.order;

import java.util.UUID;

public record NewOrderItemDto(
        UUID productId,
        Integer quantity
) {
}
