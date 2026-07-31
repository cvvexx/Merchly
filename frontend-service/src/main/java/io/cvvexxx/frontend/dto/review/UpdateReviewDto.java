package io.cvvexxx.frontend.dto.review;

import java.util.UUID;

public record UpdateReviewDto(
        UUID reviewId,
        int rating,
        String comment
) {
}
