package io.cvvexxx.orders.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record NewOrderItemDto(
        @NotNull(message = "{orders.create.item.product_id.is_null}")
        UUID productId,

        @NotNull(message = "{orders.create.item.quantity.is_null}")
        @Min(value = 1, message = "{orders.create.item.quantity.invalid}")
        Integer quantity
) {
}