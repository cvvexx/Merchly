package io.cvvexxx.frontend.dto.review;

import java.util.UUID;

public record ReviewDto(
        UUID reviewId,
        UUID productId,
        UUID userId,
        int rating,
        String comment
) {
}
