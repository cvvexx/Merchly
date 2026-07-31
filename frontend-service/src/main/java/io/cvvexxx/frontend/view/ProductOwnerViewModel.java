package io.cvvexxx.frontend.view;

import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewStatsDto;

import java.util.List;

public record ProductOwnerViewModel(
        Product product,
        ProductOwnerDto user,
        String productImageUrl,
        String userAvatarUrl,
        long reviewsCount,
        double averageRating
) {
}
