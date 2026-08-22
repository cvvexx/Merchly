package io.cvvexxx.orders.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record NewOrderDto(
        @NotEmpty(message = "{orders.create.items.is_empty}")
        @Valid
        List<NewOrderItemDto> items,

        @Size(max = 255, message = "{orders.create.delivery_address.invalid}")
        String deliveryAddress,

        @Size(max = 500, message = "{orders.create.comment.too_long}")
        String comment
) {
}