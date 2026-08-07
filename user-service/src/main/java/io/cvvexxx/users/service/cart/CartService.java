package io.cvvexxx.users.service.cart;

import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.dto.cart.CartItemDto;

import java.util.List;
import java.util.UUID;

public interface CartService {

    void addItemToCart(AddToCartDto addToCartDto, UUID currentUserId);

    List<CartItemDto> getCartItems(UUID currentUserId);

    void deleterItemFromCart(UUID productId, UUID currentUserId);
}
