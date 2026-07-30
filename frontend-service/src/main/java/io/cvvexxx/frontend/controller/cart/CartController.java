package io.cvvexxx.frontend.controller.cart;

import io.cvvexxx.frontend.client.product.internal.ProductsInternalRestClient;
import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.dto.product.AddToCartDto;
import io.cvvexxx.frontend.dto.product.CartItemDto;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import io.cvvexxx.frontend.view.CartItemView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final UserPublicRestClient userPublicRestClient;
    private final ProductsInternalRestClient productsInternalRestClient;
    private final ImageUrlFormatter imageUrlFormatter;

    @GetMapping
    public String getCartPage(Model model) {
        log.info("Request received to render cart page");

        List<CartItemDto> cartItems = userPublicRestClient.getCartItems();
        log.debug("Retrieved {} items from user cart service", cartItems.size());

        if (cartItems.isEmpty()) {
            log.info("Cart is empty for current user");
            model.addAttribute("items", List.of());
            model.addAttribute("totalPrice", BigDecimal.ZERO);
            return "cart/cart";
        }

        List<UUID> productIds = cartItems.stream()
                .map(CartItemDto::productId)
                .toList();

        log.debug("Fetching details for product IDs: {}", productIds);
        List<Product> products = productsInternalRestClient.findAllProductsByIds(productIds);
        log.debug("Successfully fetched {} products from product service", products.size());

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

        log.info("Rendering cart page with {} items, total price: {}", viewItems.size(), totalCartPrice);
        model.addAttribute("items", viewItems);
        model.addAttribute("totalPrice", totalCartPrice);
        return "cart/cart";
    }

    @PostMapping("/add")
    @ResponseBody
    public ResponseEntity<Void> addProductToCart(
            @RequestBody AddToCartDto addToCartDto
    ) {
        log.info("Request to add product to cart: productId={}, quantity={}",
                addToCartDto.productId(), addToCartDto.quantity());

        userPublicRestClient.addProductToCart(addToCartDto);

        log.info("Product successfully added to cart for productId={}", addToCartDto.productId());
        return ResponseEntity.ok().build();
    }
}