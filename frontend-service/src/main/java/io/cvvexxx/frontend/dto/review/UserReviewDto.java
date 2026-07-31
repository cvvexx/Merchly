package io.cvvexxx.frontend.dto.review;

import java.util.UUID;

public record UserReviewDto(
        UUID reviewId,
        UUID productId,
        UUID userId,
        String username,
        int rating,
        String comment
) {
}
