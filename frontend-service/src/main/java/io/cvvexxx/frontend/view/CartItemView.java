package io.cvvexxx.frontend.view;

import io.cvvexxx.frontend.dto.product.Product;

import java.math.BigDecimal;

public record CartItemView(
        Product product,
        int quantity,
        BigDecimal subtotal,
        String productImageUrl
) {
    public CartItemView(Product product, int quantity, String productImageUrl) {
        this(
                product,
                quantity,
                product.price().multiply(BigDecimal.valueOf(quantity)),
                productImageUrl
        );
    }
}