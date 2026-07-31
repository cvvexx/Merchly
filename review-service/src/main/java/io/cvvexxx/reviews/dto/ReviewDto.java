package io.cvvexxx.reviews.dto;

import java.util.UUID;

public record ReviewDto(
        UUID reviewId,
        UUID productId,
        UUID userId,
        int rating,
        String comment
) {
}
