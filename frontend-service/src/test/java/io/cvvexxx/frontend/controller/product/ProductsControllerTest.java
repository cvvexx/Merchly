package io.cvvexxx.frontend.controller.product;

import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.controller.product.payload.NewProductPayload;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ConcurrentModel;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductsControllerTest {


    @Mock
    ProductsPublicRestClient productsPublicRestClient;
    @Mock
    UserInternalRestClient userInternalRestClient;
    @Mock
    ReviewsRestClient reviewsRestClient;
    @Mock
    ImageUrlFormatter imageUrlFormatter;

    @InjectMocks
    ProductsController productsController;

    @Test
    @DisplayName("createProduct: valid request returns redirect to product page")
    void createProduct_RequestIsValid_ReturnsRedirectionToProductPage() {
        // given
        UUID creatorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        var payload = new NewProductPayload(
                "new product title",
                "new product description",
                BigDecimal.TEN,
                creatorId
        );

        var model = new ConcurrentModel();
        var image = new MockMultipartFile(
                "image",
                "image.png",
                "image/png",
                "123".getBytes()
        );

        var token = new KeycloakJwtAuthenticationToken(
                creatorId.toString(),
                creatorId,
                "1234",
                "123",
                Stream.of("ROLE_USER")
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList()
        );

        var createdProduct = new Product(
                productId,
                "new product title",
                "new product description",
                BigDecimal.TEN,
                image.getOriginalFilename(),
                creatorId
        );

        doReturn(createdProduct)
                .when(productsPublicRestClient)
                .createProduct("new product title", "new product description", BigDecimal.TEN, image, creatorId);

        // when
        var result = productsController.createProduct(payload, image, model, token);

        // then
        assertEquals("redirect:/catalogue/products/" + productId, result);
        verify(productsPublicRestClient).createProduct("new product title", "new product description",
                BigDecimal.TEN, image, creatorId);
        verifyNoMoreInteractions(productsPublicRestClient);
    }

    @Test
    @DisplayName("create product returns errors page if request is invalid")
    void createProduct_RequestIsInvalid_ReturnsProductFromWithErrors() {
        //given
        UUID creatorId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        var model = new ConcurrentModel();
        var image = new MockMultipartFile(
                "image",
                "image.png",
                "image/png",
                "123".getBytes()
        );
        var payload = new NewProductPayload("    ", null, BigDecimal.ZERO, creatorId);
        var token = new KeycloakJwtAuthenticationToken(
                creatorId.toString(),
                creatorId,
                "1234",
                "123",
                Stream.of("ROLE_USER")
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList()
        );

        doThrow(new BadRequestException(List.of("error1", "error2")))
                .when(productsPublicRestClient)
                .createProduct("    ", null, BigDecimal.ZERO, image, creatorId);
        //when
        var result = productsController.createProduct(
                payload,
                image,
                model,
                token
        );
        //then
        assertEquals("catalogue/products/new_product", result);
        assertEquals(payload, model.getAttribute("payload"));
        assertEquals(List.of("error1", "error2"), model.getAttribute("errors"));

        verify(productsPublicRestClient).createProduct("    ", null, BigDecimal.ZERO, image, creatorId);
        verifyNoMoreInteractions(productsPublicRestClient);
    }


}