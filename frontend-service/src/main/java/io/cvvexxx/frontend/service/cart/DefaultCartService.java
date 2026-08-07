package io.cvvexxx.frontend.service.cart;

import io.cvvexxx.frontend.client.product.internal.ProductsInternalRestClient;
import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.dto.cart.CartPageData;
import io.cvvexxx.frontend.dto.product.CartItemDto;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import io.cvvexxx.frontend.view.CartItemView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultCartService implements CartService {

    private final UserPublicRestClient userPublicRestClient;
    private final ProductsInternalRestClient productsInternalRestClient;
    private final ImageUrlFormatter imageUrlFormatter;

    @Override
    public CartPageData getCartPage() {
        List<CartItemDto> cartItems = userPublicRestClient.getCartItems();

        if (cartItems.isEmpty()) {
            return new CartPageData(List.of(), BigDecimal.ZERO);
        }

        List<UUID> productIds = cartItems.stream()
                .map(CartItemDto::productId)
                .toList();

        List<Product> products = productsInternalRestClient.findAllProductsByIds(productIds);

        Map<UUID, Product> productMap = products.stream()
                .collect(Collectors.toMap(Product::id, Function.identity()));

        List<CartItemView> viewItems = new ArrayList<>();
        BigDecimal totalCartPrice = BigDecimal.ZERO;

        for (CartItemDto cartItem : cartItems) {
            Product product = productMap.get(cartItem.productId());

            if (product != null) {
                CartItemView view = new CartItemView(
                        product, cartItem.quantity(), imageUrlFormatter.getProductImageUrl(product.imageFileName())
                );
                viewItems.add(view);
                totalCartPrice = totalCartPrice.add(view.subtotal());
            } else {
                log.warn("Product with ID {} was found in user cart, but not returned by product service", cartItem.productId());
            }
        }

        return new CartPageData(viewItems, totalCartPrice);
    }

}
