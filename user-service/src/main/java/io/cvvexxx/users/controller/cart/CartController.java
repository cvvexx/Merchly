package io.cvvexxx.users.controller.cart;


import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.dto.cart.CartItemDto;
import io.cvvexxx.users.service.cart.DefaultCartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users/cart")
@RequiredArgsConstructor
@Slf4j
public class CartController {

    private final DefaultCartService cartService;

    @GetMapping
    public ResponseEntity<List<CartItemDto>> getCartItems(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("sub"));
        log.info("getting cart items from user {}", currentUserId);
        List<CartItemDto> items = cartService.getCartItems(currentUserId);

        return ResponseEntity.ok(items);
    }

    @PostMapping
    public ResponseEntity<Void> addProductToCart(
            @Valid @RequestBody AddToCartDto addToCartDto,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("sub"));
        cartService.addItemToCart(addToCartDto, currentUserId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("{productId}")
    public ResponseEntity<Void> removeProductFromCart(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID productId
    ) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("sub"));

        cartService.deleteritemfromcart(productId, currentUserId);

        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID currentUserId = UUID.fromString(jwt.getClaimAsString("sub"));

        cartService.clearCart(currentUserId);

        return ResponseEntity.noContent().build();
    }

}
