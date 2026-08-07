package io.cvvexxx.reviews.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record NewReviewDto(
        @NotNull(message = "review.create.productId_is_null")
        UUID productId,

        @NotNull(message = "review.create.rating_is_null")
        @Min(value = 1, message = "review.create.rating_below_1")
        @Max(value = 5, message = "review.create.rating_above_5")
        int rating,

        @Size(max = 2000, message = "review.create.comment_is_too_large")
        String comment
) {
}
