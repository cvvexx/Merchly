package io.cvvexxx.products.controller.payload;


import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record NewProductPayload(

        @NotNull(message = "{catalogue.products.create.errors.title_is_null}")
        @Size(min = 3, max = 50, message = "{catalogue.products.create.errors.title_size_is_invalid}")
        String title,

        @Size(max = 1000, message = "{catalogue.products.create.errors.description_size_is_invalid}")
        String description,

        @NotNull(message = "{catalogue.products.create.errors.price_is_null}")
        @Positive(message = "{catalogue.products.create.errors.price_is_negative_or_zero}")
        BigDecimal price,

        @NotNull
        UUID createdBy
) {
}
