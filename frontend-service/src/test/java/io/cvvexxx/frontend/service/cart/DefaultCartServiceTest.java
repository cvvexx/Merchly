package io.cvvexxx.frontend.service.cart;

import io.cvvexxx.frontend.client.product.internal.ProductsInternalRestClient;
import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.dto.cart.CartPageData;
import io.cvvexxx.frontend.dto.product.CartItemDto;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultCartServiceTest {

    @Mock
    private UserPublicRestClient userPublicRestClient;

    @Mock
    private ProductsInternalRestClient productsInternalRestClient;

    @Mock
    private ImageUrlFormatter imageUrlFormatter;

    @InjectMocks
    private DefaultCartService cartService;

    @Test
    @DisplayName("если корзина пуста, возвращает пустые данные и не обращается к product-service")
    void getCartPage_WhenCartIsEmpty_ShouldReturnEmptyDataWithoutCallingProductService() {
        when(userPublicRestClient.getCartItems()).thenReturn(List.of());

        CartPageData result = cartService.getCartPage();

        assertEquals(List.of(), result.viewItems());
        assertEquals(BigDecimal.ZERO, result.totalCartPrice());
        verifyNoInteractions(productsInternalRestClient);
    }

    @Test
    @DisplayName("считает subtotal по каждому товару и суммарную стоимость корзины")
    void getCartPage_ShouldBuildViewItemsAndComputeTotalPrice() {
        UUID productId = UUID.randomUUID();
        CartItemDto cartItem = new CartItemDto(productId, 3);
        when(userPublicRestClient.getCartItems()).thenReturn(List.of(cartItem));

        Product product = new Product(productId, "title", "desc", 10, new BigDecimal("20.00"), "image.png", UUID.randomUUID());
        when(productsInternalRestClient.findAllProductsByIds(List.of(productId))).thenReturn(List.of(product));
        when(imageUrlFormatter.getProductImageUrl("image.png")).thenReturn("/img/product.png");

        CartPageData result = cartService.getCartPage();

        assertEquals(1, result.viewItems().size());
        assertEquals(3, result.viewItems().get(0).quantity());
        assertEquals(new BigDecimal("60.00"), result.viewItems().get(0).subtotal());
        assertEquals("/img/product.png", result.viewItems().get(0).productImageUrl());
        assertEquals(new BigDecimal("60.00"), result.totalCartPrice());
    }

    @Test
    @DisplayName("если товар из корзины не найден в product-service, пропускает его без падения")
    void getCartPage_WhenProductMissingFromProductService_ShouldSkipItWithoutFailing() {
        UUID missingProductId = UUID.randomUUID();
        CartItemDto cartItem = new CartItemDto(missingProductId, 1);
        when(userPublicRestClient.getCartItems()).thenReturn(List.of(cartItem));
        when(productsInternalRestClient.findAllProductsByIds(List.of(missingProductId))).thenReturn(List.of());

        CartPageData result = cartService.getCartPage();

        assertEquals(List.of(), result.viewItems());
        assertEquals(BigDecimal.ZERO, result.totalCartPrice());
    }
}
