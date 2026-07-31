package io.cvvexxx.frontend.view;

import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;

public record ProductOwnerViewModel(
        Product product,
        ProductOwnerDto user,
        double averageRating,
        int totalReviews,
        String productImageUrl,
        String userAvatarUrl
) {
}
