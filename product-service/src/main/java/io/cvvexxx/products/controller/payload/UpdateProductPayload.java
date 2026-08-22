package io.cvvexxx.products.controller.payload;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateProductPayload(

        @NotNull(message = "{catalogue.products.update.errors.title_is_null}")
        @Size(min = 3, max = 50, message = "{catalogue.products.update.errors.title_size_is_invalid}")
        String title,

        @Size(max = 1000, message = "{catalogue.products.update.errors.description_size_is_invalid}")
        String description,

        @PositiveOrZero(message = "{catalogue.products.update.errors.quantity_is_negative}")
        @NotNull(message = "{catalogue.products.update.errors.quantity_is_null}")
        Integer quantity,

        @NotNull(message = "{catalogue.products.create.errors.price_is_null}")
        @Positive(message = "{catalogue.products.create.errors.price_is_negative_or_zero}")
        BigDecimal price
) {
}
