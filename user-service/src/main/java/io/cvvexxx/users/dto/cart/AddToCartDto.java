package io.cvvexxx.users.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AddToCartDto(
        @NotNull(message = "{cart.add.productId.required}")
        UUID productId,

        @NotNull(message = "{cart.add.quantity.required}")
        @Min(value = 1, message = "{cart.add.quantity.at_least_one}")
        Integer quantity
) {
}