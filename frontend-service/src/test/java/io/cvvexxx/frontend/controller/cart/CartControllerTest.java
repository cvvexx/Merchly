package io.cvvexxx.frontend.controller.cart;

import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.dto.cart.CartPageData;
import io.cvvexxx.frontend.dto.product.AddToCartDto;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.service.cart.DefaultCartService;
import io.cvvexxx.frontend.view.CartItemView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.ConcurrentModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartControllerTest {

    @Mock
    private UserPublicRestClient userPublicRestClient;

    @Mock
    private DefaultCartService defaultCartService;

    @InjectMocks
    private CartController controller;

    @Test
    @DisplayName("getCartPage: наполняет модель товарами и итоговой суммой корзины")
    void getCartPage_ShouldPopulateModelWithItemsAndTotalPrice() {
        // given
        Product product = new Product(UUID.randomUUID(), "title", "desc", 1, BigDecimal.TEN, "image.png", UUID.randomUUID());
        CartItemView item = new CartItemView(product, 2, "/img/product.png");
        CartPageData pageData = new CartPageData(List.of(item), new BigDecimal("20.00"));
        when(defaultCartService.getCartPage()).thenReturn(pageData);
        var model = new ConcurrentModel();

        // when
        String result = controller.getCartPage(model);

        // then
        assertEquals("cart/cart", result);
        assertEquals(List.of(item), model.getAttribute("items"));
        assertEquals(new BigDecimal("20.00"), model.getAttribute("totalPrice"));
    }

    @Test
    @DisplayName("addProductToCart: делегирует добавление товара в корзину user-service и возвращает 200 OK")
    void addProductToCart_ShouldDelegateToUserServiceAndReturnOk() {
        // given
        AddToCartDto dto = new AddToCartDto(UUID.randomUUID(), 3);

        // when
        ResponseEntity<Void> response = controller.addProductToCart(dto);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(userPublicRestClient).addProductToCart(dto);
    }

    @Test
    @DisplayName("deleteProductFromCart: делегирует удаление товара из корзины и возвращает 204 No Content")
    void deleteProductFromCart_ShouldDelegateToUserServiceAndReturnNoContent() {
        // given
        UUID productId = UUID.randomUUID();

        // when
        ResponseEntity<Void> response = controller.deleteProductFromCart(productId);

        // then
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(userPublicRestClient).deleteProductFromCart(productId);
    }
}
