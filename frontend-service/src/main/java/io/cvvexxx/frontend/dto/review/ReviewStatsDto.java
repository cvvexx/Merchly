package io.cvvexxx.frontend.dto.review;

import java.util.UUID;

public record ReviewStatsDto(
        UUID productId,
        double averageRating,
        long totalReviews
) {
}
