package io.cvvexxx.products.controller.publIc;

import io.cvvexxx.products.controller.payload.UpdateProductPayload;
import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.service.product.ProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductRestControllerTest {

    @Mock
    private ProductService productService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ProductRestController controller;

    @Test
    @DisplayName("findProduct: делегирует поиск товара в сервис")
    void findProduct_ShouldDelegateToService() {
        UUID productId = UUID.randomUUID();
        ProductDto product = new ProductDto(productId, "Title", "Desc", 1, BigDecimal.TEN, "image.png", UUID.randomUUID());
        when(productService.findProductById(productId)).thenReturn(product);

        ProductDto result = controller.findProduct(productId);

        assertEquals(product, result);
        verify(productService).findProductById(productId);
    }

    @Test
    @DisplayName("updateProduct: при ошибках валидации выбрасывает BindException и не обновляет товар")
    void updateProduct_WhenBindingResultHasErrors_ShouldThrowBindException() {
        UUID productId = UUID.randomUUID();
        UpdateProductPayload payload = new UpdateProductPayload("Ab", "Desc", 1, BigDecimal.TEN);
        var bindingResult = new BeanPropertyBindingResult(payload, "updateProductPayload");
        bindingResult.reject("title", "size must be between 3 and 50");

        assertThrows(BindException.class, () ->
                controller.updateProduct(productId, payload, null, bindingResult)
        );

        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("updateProduct: при валидном запросе обновляет товар и возвращает 204")
    void updateProduct_WhenValid_ShouldUpdateProductAndReturnNoContent() throws BindException {
        UUID productId = UUID.randomUUID();
        UpdateProductPayload payload = new UpdateProductPayload("Title", "Desc", 1, BigDecimal.TEN);
        var bindingResult = new BeanPropertyBindingResult(payload, "updateProductPayload");
        MockMultipartFile image = new MockMultipartFile("image", "avatar.png", "image/png", "bytes".getBytes());

        ResponseEntity<Void> response = controller.updateProduct(productId, payload, image, bindingResult);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService).updateProduct(productId, payload.title(), payload.description(),
                payload.quantity(), payload.price(), image);
    }

    @Test
    @DisplayName("deleteProduct: делегирует удаление в сервис и возвращает 204")
    void deleteProduct_ShouldDelegateToServiceAndReturnNoContent() {
        UUID productId = UUID.randomUUID();

        ResponseEntity<Void> response = controller.deleteProduct(productId);

        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
        verify(productService).deleteProduct(productId);
    }

    @Test
    @DisplayName("handleNoSuchElementException: сопоставляет исключение с 404 и локализованным сообщением")
    void handleNoSuchElementException_ShouldReturnNotFoundWithLocalizedMessage() {
        NoSuchElementException exception = new NoSuchElementException("catalogue.errors.product.not_found");
        Locale locale = Locale.ENGLISH;
        when(messageSource.getMessage("catalogue.errors.product.not_found", new Object[0],
                "catalogue.errors.product.not_found", locale)).thenReturn("Product not found");

        ResponseEntity<ProblemDetail> response = controller.handleNoSuchElementException(exception, locale);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Product not found", response.getBody().getDetail());
    }
}
