package io.cvvexxx.frontend.controller.product;

import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.controller.product.payload.UpdateProductPayload;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductPageData;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.service.product.DefaultProductService;
import io.cvvexxx.frontend.view.ProductDetailsViewModel;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ConcurrentModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductsPublicRestClient productsPublicRestClient;

    @Mock
    private MessageSource messageSource;

    @Mock
    private DefaultProductService defaultProductService;

    @InjectMocks
    private ProductController controller;

    @Test
    @DisplayName("getProductPage: наполняет модель данными из сервиса и возвращает вьюху товара")
    void getProductPage_ShouldPopulateModelFromService() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = product(productId);
        var model = new ConcurrentModel();
        var pageable = org.springframework.data.domain.PageRequest.of(0, 5);
        var token = tokenWithRoles(List.of("ROLE_ADMIN"));

        ProductDetailsViewModel viewModel = mock(ProductDetailsViewModel.class);
        ProductPageData pageData = new ProductPageData(viewModel, true, "authUser");
        when(defaultProductService.getProductPage(product, pageable, token)).thenReturn(pageData);

        // when
        String result = controller.getProductPage(product, pageable, model, token);

        // then
        assertEquals("catalogue/products/product", result);
        assertEquals(true, model.getAttribute("isAdmin"));
        assertEquals("authUser", model.getAttribute("authUsername"));
        assertEquals(viewModel, model.getAttribute("data"));
    }

    @Test
    @DisplayName("getProductEditPage: возвращает страницу редактирования")
    void getProductEditPage_ShouldReturnEditPage() {
        // given / when
        String result = controller.getProductEditPage();

        // then
        assertEquals("catalogue/products/edit", result);
    }

    @Test
    @DisplayName("deleteProduct: удаляет товар и делает редирект на список товаров")
    void deleteProduct_ShouldDeleteAndRedirectToList() {
        // given
        UUID productId = UUID.randomUUID();
        Product product = product(productId);

        // when
        String result = controller.deleteProduct(product);

        // then
        assertEquals("redirect:/catalogue/products/list", result);
        verify(productsPublicRestClient).deleteProduct(productId);
    }

    @Test
    @DisplayName("handleNoSuchElementException: возвращает 404 и локализованное сообщение об ошибке")
    void handleNoSuchElementException_ShouldSetNotFoundStatusAndLocalizedMessage() {
        // given
        var exception = new NoSuchElementException("catalogue.errors.product.not_found");
        var response = new MockHttpServletResponse();
        var model = new ConcurrentModel();
        Locale locale = Locale.ENGLISH;
        when(messageSource.getMessage("catalogue.errors.product.not_found", new Object[0],
                "catalogue.errors.product.not_found", locale)).thenReturn("Product not found");

        // when
        String result = controller.handleNoSuchElementException(exception, locale, response, model);

        // then
        assertEquals("error/404", result);
        assertEquals(HttpServletResponse.SC_NOT_FOUND, response.getStatus());
        assertEquals("Product not found", model.getAttribute("error"));
    }

    @Test
    @DisplayName("accessDenied: возвращает страницу 403")
    void accessDenied_ShouldReturn403Page() {
        // given / when
        String result = controller.accessDenied();

        // then
        assertEquals("error/403", result);
    }

    private Product product(UUID productId) {
        return new Product(productId, "title", "desc", 5, BigDecimal.TEN, "image.png", UUID.randomUUID());
    }

    private KeycloakJwtAuthenticationToken tokenWithRoles(List<String> roles) {
        return new KeycloakJwtAuthenticationToken(
                "authUser",
                UUID.randomUUID(),
                "access-token",
                "refresh-token",
                roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList()
        );
    }

    @Nested
    @DisplayName("product (@ModelAttribute)")
    class ProductModelAttributeTests {

        @Test
        @DisplayName("если товар найден, возвращает его")
        void product_WhenFound_ShouldReturnProduct() {
            // given
            UUID productId = UUID.randomUUID();
            Product product = product(productId);
            when(productsPublicRestClient.findProductById(productId)).thenReturn(java.util.Optional.of(product));

            // when
            Product result = controller.product(productId);

            // then
            assertEquals(product, result);
        }

        @Test
        @DisplayName("если товар не найден, выбрасывает NoSuchElementException")
        void product_WhenNotFound_ShouldThrowNoSuchElementException() {
            // given
            UUID productId = UUID.randomUUID();
            when(productsPublicRestClient.findProductById(productId)).thenReturn(java.util.Optional.empty());

            // when / then
            assertThrows(NoSuchElementException.class, () -> controller.product(productId));
        }
    }

    @Nested
    @DisplayName("updateProduct")
    class UpdateProductTests {

        @Test
        @DisplayName("при успешном обновлении делает редирект на страницу товара")
        void updateProduct_WhenValid_ShouldRedirectToProductPage() {
            // given
            UUID productId = UUID.randomUUID();
            Product product = product(productId);
            var payload = new UpdateProductPayload("title", "desc", 5, BigDecimal.TEN);
            var image = new MockMultipartFile("image", "image.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();

            // when
            String result = controller.updateProduct(product, image, payload, model);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            verify(productsPublicRestClient).updateProduct(
                    productId, "title", "desc", 5, BigDecimal.TEN, image
            );
        }

        @Test
        @DisplayName("при ошибке валидации возвращает страницу редактирования с ошибками")
        void updateProduct_WhenBadRequest_ShouldReturnEditPageWithErrors() {
            // given
            UUID productId = UUID.randomUUID();
            Product product = product(productId);
            var payload = new UpdateProductPayload("", null, -1, BigDecimal.ZERO);
            var image = new MockMultipartFile("image", "image.png", "image/png", "123".getBytes());
            var model = new ConcurrentModel();

            doThrow(new BadRequestException(List.of("error1")))
                    .when(productsPublicRestClient)
                    .updateProduct(productId, "", null, -1, BigDecimal.ZERO, image);

            // when
            String result = controller.updateProduct(product, image, payload, model);

            // then
            assertEquals("catalogue/products/edit", result);
            assertEquals(payload, model.getAttribute("payload"));
            assertEquals(List.of("error1"), model.getAttribute("errors"));
        }
    }
}
