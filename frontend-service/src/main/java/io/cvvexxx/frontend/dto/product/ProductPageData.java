package io.cvvexxx.frontend.dto.product;

import io.cvvexxx.frontend.view.ProductDetailsViewModel;

public record ProductPageData(
        ProductDetailsViewModel viewModel,
        boolean isAdmin,
        String authUsername
) {
}
