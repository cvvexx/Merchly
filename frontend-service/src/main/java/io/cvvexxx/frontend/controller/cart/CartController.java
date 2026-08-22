package io.cvvexxx.frontend.controller.cart;

import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.dto.product.AddToCartDto;
import io.cvvexxx.frontend.dto.product.CartItemDto;
import io.cvvexxx.frontend.service.cart.DefaultCartService;
import io.cvvexxx.frontend.view.CartItemView;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final UserPublicRestClient userPublicRestClient;
    private final DefaultCartService defaultCartService;

    @GetMapping
    public String getCartPage(Model model) {
        var cartPageData = defaultCartService.getCartPage();
        List<CartItemView> viewItems = cartPageData.viewItems();
        BigDecimal totalCartPrice = cartPageData.totalCartPrice();

        model.addAttribute("items", viewItems);
        model.addAttribute("totalPrice", totalCartPrice);
        return "cart/cart";
    }

    /**
     * Число товаров в корзине для счётчика в панели навигации.
     * Отдельный лёгкий метод: карточки товаров здесь не нужны.
     */
    @GetMapping("/count")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getCartItemsCount() {
        int count = userPublicRestClient.getCartItems().stream()
                .mapToInt(CartItemDto::quantity)
                .sum();

        return ResponseEntity.ok(Map.of("count", count));
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

    @DeleteMapping("{productId}")
    @ResponseBody
    public ResponseEntity<Void> deleteProductFromCart(@PathVariable("productId") UUID productId) {
        log.info("Request to delete product from cart for productId={}", productId);

        userPublicRestClient.deleteProductFromCart(productId);

        return ResponseEntity.noContent().build();
    }
}