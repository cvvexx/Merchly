package io.cvvexxx.frontend.dto.cart;

import io.cvvexxx.frontend.view.CartItemView;

import java.math.BigDecimal;
import java.util.List;

public record CartPageData(
        List<CartItemView> viewItems,
        BigDecimal totalCartPrice
) {
}
