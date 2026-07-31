package io.cvvexxx.reviews.dto;

import java.util.UUID;

public record ReviewStatsDto(
        UUID productId,
        double averageRating,
        long totalReviews
) {
}