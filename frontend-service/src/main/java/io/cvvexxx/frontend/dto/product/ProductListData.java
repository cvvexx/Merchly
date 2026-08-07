package io.cvvexxx.frontend.dto.product;

import io.cvvexxx.frontend.view.ProductOwnerViewModel;

import java.util.List;

public record ProductListData(
        List<ProductOwnerViewModel> viewModels
) {
}
