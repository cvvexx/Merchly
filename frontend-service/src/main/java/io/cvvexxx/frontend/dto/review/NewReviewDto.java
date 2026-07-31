package io.cvvexxx.frontend.dto.review;

import java.util.UUID;

public record NewReviewDto(
        UUID productId,
        int rating,
        String comment

) {
}
