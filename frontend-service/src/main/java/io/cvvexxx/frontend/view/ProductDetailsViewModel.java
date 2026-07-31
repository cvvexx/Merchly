package io.cvvexxx.frontend.view;

import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import org.springframework.data.domain.Page;

public record ProductDetailsViewModel(
        Product product,
        ProductOwnerDto user,
        String productImageUrl,
        String userAvatarUrl,
        Page<ReviewDto> reviews,
        long reviewsCount,
        double averageRating
) {
}