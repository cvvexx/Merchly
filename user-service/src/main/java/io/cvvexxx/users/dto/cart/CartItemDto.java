package io.cvvexxx.users.dto.cart;

import java.util.UUID;

public record CartItemDto(
        UUID productId,
        int quantity
) {
}
