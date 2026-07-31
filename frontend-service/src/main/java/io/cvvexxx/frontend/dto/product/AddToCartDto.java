package io.cvvexxx.frontend.dto.product;

import java.util.UUID;

public record AddToCartDto(
        UUID productId,
        Integer quantity
) {
}
