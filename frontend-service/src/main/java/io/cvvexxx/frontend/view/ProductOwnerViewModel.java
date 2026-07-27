package io.cvvexxx.frontend.view;

import io.cvvexxx.frontend.dto.Product;
import io.cvvexxx.frontend.dto.ProductOwnerDto;

public record ProductOwnerViewModel(
        Product product,
        ProductOwnerDto user,
        String imageUrl
) {
}
