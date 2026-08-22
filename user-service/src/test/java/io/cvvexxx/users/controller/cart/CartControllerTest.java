package io.cvvexxx.users.controller.cart;

import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.dto.cart.CartItemDto;
import io.cvvexxx.users.service.cart.DefaultCartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private DefaultCartService cartService;

    @InjectMocks
    private CartController controller;

    @Test
    @DisplayName("getCartItems: извлекает userId из sub-claim и возвращает список товаров корзины")
    void getCartItems_ShouldReturnItemsForCurrentUser() {
        // given
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        List<CartItemDto> items = List.of(new CartItemDto(UUID.randomUUID(), 2));
        when(cartService.getCartItems(userId)).thenReturn(items);

        // when
        ResponseEntity<List<CartItemDto>> response = controller.getCartItems(jwt);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(items, response.getBody());
        verify(cartService).getCartItems(userId);
    }

    @Test
    @DisplayName("addProductToCart: делегирует добавление товара в корзину текущего пользователя")
    void addProductToCart_ShouldDelegateToServiceForCurrentUser() {
        // given
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);
        AddToCartDto dto = new AddToCartDto(UUID.randomUUID(), 3);

        // when
        ResponseEntity<Void> response = controller.addProductToCart(dto, jwt);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(cartService).addItemToCart(dto, userId);
    }

    @Test
    @DisplayName("removeProductFromCart: делегирует удаление товара текущего пользователя и возвращает 204")
    void removeProductFromCart_ShouldDeleteItemForCurrentUser() {
        // given
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        // when
        ResponseEntity<Void> response = controller.removeProductFromCart(jwt, productId);

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cartService).deleteritemfromcart(productId, userId);
    }

    @Test
    @DisplayName("clearCart: очищает корзину текущего пользователя и возвращает 204")
    void clearCart_ShouldClearCartForCurrentUser() {
        // given
        UUID userId = UUID.randomUUID();
        Jwt jwt = jwtFor(userId);

        // when
        ResponseEntity<Void> response = controller.clearCart(jwt);

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(cartService).clearCart(userId);
    }

    private Jwt jwtFor(UUID userId) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .build();
    }
}
