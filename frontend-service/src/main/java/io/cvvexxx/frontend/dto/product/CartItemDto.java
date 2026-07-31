package io.cvvexxx.frontend.dto.product;

import java.util.UUID;

public record CartItemDto(
        UUID productId,
        int quantity
) {
}
