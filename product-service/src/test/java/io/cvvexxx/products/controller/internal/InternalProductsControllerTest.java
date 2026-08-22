package io.cvvexxx.products.controller.internal;

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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalProductsControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private InternalProductsController controller;

    @Test
    @DisplayName("если ids не переданы (null), возвращает пустой список и не обращается к сервису")
    void findAllProducts_WhenIdsIsNull_ShouldReturnEmptyListWithoutCallingService() {
        // given
        List<UUID> ids = null;

        // when
        ResponseEntity<List<ProductDto>> response = assertDoesNotThrow(() -> controller.findAllProducts(ids));

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("если ids — пустой список, возвращает пустой список и не обращается к сервису")
    void findAllProducts_WhenIdsIsEmpty_ShouldReturnEmptyListWithoutCallingService() {
        // given
        List<UUID> ids = List.of();

        // when
        ResponseEntity<List<ProductDto>> response = controller.findAllProducts(ids);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(), response.getBody());
        verifyNoInteractions(productService);
    }

    @Test
    @DisplayName("если ids переданы, делегирует поиск в сервис")
    void findAllProducts_WhenIdsProvided_ShouldDelegateToService() {
        // given
        UUID productId = UUID.randomUUID();
        List<UUID> ids = List.of(productId);
        ProductDto product = new ProductDto(productId, "Title", "Desc", 1, BigDecimal.TEN, "image.png", UUID.randomUUID());
        when(productService.findAllByIdIn(ids)).thenReturn(List.of(product));

        // when
        ResponseEntity<List<ProductDto>> response = controller.findAllProducts(ids);

        // then
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(List.of(product), response.getBody());
        verify(productService).findAllByIdIn(ids);
    }
}
