package io.cvvexxx.frontend.view;

import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;

public record ProductOwnerViewModel(
        Product product,
        ProductOwnerDto user,
        String productImageUrl,
        String userAvatarUrl
) {
}
