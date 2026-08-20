package io.cvvexxx.products.controller.publIc;

import io.cvvexxx.products.controller.payload.NewProductPayload;
import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.service.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductsRestControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductsRestController controller;

    @Test
    @DisplayName("getAllProducts: делегирует поиск в сервис с переданным фильтром")
    void getAllProducts_ShouldDelegateToServiceWithFilter() {
        // given
        String filter = "shirt";
        ProductDto product = new ProductDto(UUID.randomUUID(), "T-Shirt", "Desc", 1, BigDecimal.TEN, "image.png", UUID.randomUUID());
        when(productService.findAllProducts(filter)).thenReturn(List.of(product));

        // when
        ResponseEntity<List<ProductDto>> response = controller.getAllProducts(filter);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(product), response.getBody());
        verify(productService).findAllProducts(filter);
    }

    @Test
    @DisplayName("createProduct: при ошибках валидации выбрасывает BindException и не создаёт товар")
    void createProduct_WhenBindingResultHasErrors_ShouldThrowBindException() {
        // given
        NewProductPayload payload = new NewProductPayload("Ab", "Desc", 1, BigDecimal.TEN, UUID.randomUUID());
        var bindingResult = new BeanPropertyBindingResult(payload, "newProductPayload");
        bindingResult.reject("title", "size must be between 3 and 50");

        // when / then
        assertThrows(BindException.class, () ->
                controller.createProduct(payload, null, UriComponentsBuilder.newInstance(), bindingResult)
        );

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("createProduct: при валидном запросе создаёт товар и возвращает Location")
    void createProduct_WhenValid_ShouldCreateProductAndReturnLocation() throws BindException {
        // given
        UUID productId = UUID.randomUUID();
        UUID createdBy = UUID.randomUUID();
        NewProductPayload payload = new NewProductPayload("Title", "Desc", 1, BigDecimal.TEN, createdBy);
        var bindingResult = new BeanPropertyBindingResult(payload, "newProductPayload");
        MockMultipartFile image = new MockMultipartFile("image", "avatar.png", "image/png", "bytes".getBytes());
        ProductDto createdProduct = new ProductDto(productId, "Title", "Desc", 1, BigDecimal.TEN, "avatar.png", createdBy);

        when(productService.createProduct(payload.title(), payload.description(), payload.quantity(),
                payload.price(), payload.createdBy(), image)).thenReturn(createdProduct);

        // when
        ResponseEntity<?> response = controller.createProduct(
                payload, image, UriComponentsBuilder.fromUriString("http://localhost"), bindingResult
        );

        // then
        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(createdProduct, response.getBody());
        assertEquals("/api/products/" + productId, response.getHeaders().getLocation().getPath());
        verify(productService).createProduct(payload.title(), payload.description(), payload.quantity(),
                payload.price(), payload.createdBy(), image);
    }
}
