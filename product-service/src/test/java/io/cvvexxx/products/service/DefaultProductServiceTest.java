package io.cvvexxx.products.service;

import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.entity.ProcessedOrderEvent;
import io.cvvexxx.products.event.OrderItemPayload;
import io.cvvexxx.products.exception.InsufficientStockException;
import io.cvvexxx.products.repository.ProcessedOrderEventRepository;
import io.cvvexxx.products.repository.ProductRepository;
import io.cvvexxx.products.service.minio.DefaultMinioService;
import io.cvvexxx.products.service.product.DefaultProductService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DefaultProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private DefaultMinioService defaultMinioService;

    @Mock
    private ProcessedOrderEventRepository processedOrderEventRepository;

    @InjectMocks
    private DefaultProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Test
    @DisplayName("findAllProducts: обращение к методу findAll если filter пустой")
    public void ProductService_FindAllProductsWhenFilterIsBlank_ReturnFindAll() {
        //given
        String filter = "   ";
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        when(productRepository.findAll()).thenReturn(List.of(
                Product.builder()
                        .id(id1)
                        .createdAt(Instant.now())
                        .build(),
                Product.builder()
                        .id(id2)
                        .createdAt(Instant.now().minusSeconds(1))
                        .build()
        ));

        //when
        productService.findAllProducts(filter);

        //then
        verify(productRepository, times(1)).findAll();
        verifyNoMoreInteractions(productRepository);
        verify(productRepository, never()).findAllByTitleContainingIgnoreCase(anyString());
    }

    @Test
    @DisplayName("findAllProducts: поиск по фильтру, если он не пустой")
    public void ProductService_FindAllProductsWhenFilterIsNotBlank_ReturnFindAllByTitleEtc() {
        //given
        String filter = "asdf";
        var id1 = UUID.randomUUID();
        var id2 = UUID.randomUUID();
        when(productRepository.findAllByTitleContainingIgnoreCase(filter)).thenReturn(List.of(
                new ProductDto(id1, "asdf", "1234", 1, BigDecimal.ONE, "image.png", id1),
                new ProductDto(id2, "asdf", "1234", 1, BigDecimal.ONE, "image.png", id2)
        ));

        //when
        var products = productService.findAllProducts(filter);

        //then
        verify(productRepository, times(1)).findAllByTitleContainingIgnoreCase(filter);
        verifyNoMoreInteractions(productRepository);
        assertEquals(2, products.size());
    }

    @Test
    @DisplayName("createProduct: успешное создание с загрузкой изображения")
    void createProduct_WithValidImage_ShouldUploadImageAndSaveProduct() {
        // given
        String title = "Футболка Merchly";
        String description = "Классный мерч";
        Integer quantity = 1;
        BigDecimal price = new BigDecimal("1500.00");
        UUID createdBy = UUID.randomUUID();
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "avatar.png",
                "image/png",
                "some-image-bytes".getBytes()
        );
        String uploadedFileName = "uuid-avatar.png";

        when(defaultMinioService.upload(image)).thenReturn(uploadedFileName);
        when(productRepository.save(any(Product.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        // when
        ProductDto result = productService.createProduct(title, description, quantity, price, createdBy, image);

        // then
        assertNotNull(result);
        assertNotNull(result.id());
        assertEquals(title, result.title());
        assertEquals(description, result.description());
        assertEquals(price, result.price());
        assertEquals(createdBy, result.createdBy());
        assertEquals(uploadedFileName, result.imageFileName());

        verify(defaultMinioService, times(1)).upload(image);
        verify(productRepository, times(1)).save(productCaptor.capture());

        Product savedProduct = productCaptor.getValue();
        assertEquals(uploadedFileName, savedProduct.getImageFileName());
    }

    @Test
    @DisplayName("createProduct: создание без изображения (null или пустой файл)")
    void createProduct_WithoutImage_ShouldNotUploadImageAndSaveProduct() {
        // given
        String title = "Кружка";
        String description = "Керамическая";
        Integer quantity = 1;
        BigDecimal price = new BigDecimal("500.00");
        UUID createdBy = UUID.randomUUID();
        MultipartFile image = null;

        when(productRepository.save(any(Product.class))).thenAnswer(invocation ->
                invocation.getArgument(0));

        // when
        ProductDto result = productService.createProduct(title, description, quantity, price, createdBy, image);

        // then
        assertNotNull(result);
        assertNull(result.imageFileName());

        verify(defaultMinioService, never()).upload(any());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    @DisplayName("createProduct: если MinioService выбросил исключение, сохранить продукт не пытаясь")
    void createProduct_MinioThrowsException_ShouldThrowException() {
        // given
        MockMultipartFile image = new MockMultipartFile(
                "image",
                "avatar.png",
                "image/png",
                "bytes".getBytes()
        );

        when(defaultMinioService.upload(image)).thenThrow(new RuntimeException("MinIO error"));

        // when & then
        assertThrows(RuntimeException.class, () ->
                productService.createProduct("Title", "Desc", 1, BigDecimal.TEN, UUID.randomUUID(), image)
        );

        verify(defaultMinioService, times(1)).upload(image);
        verify(productRepository, never()).save(any());
    }

    @Test
    @DisplayName("deleteProduct: если продукт найден, то поставить isAvailable=false")
    public void deleteProduct_UUidIsValid_DeleteProduct() {
        //given
        UUID productId = UUID.randomUUID();
        Product product = Product.builder()
                .id(productId)
                .title("TestProduct")
                .isAvailable(true)
                .build();
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        //when
        productService.deleteProduct(productId);

        //then
        verifyNoMoreInteractions(productRepository);
        verify(productRepository, times(1)).findById(productId);
        assertEquals("TestProduct", product.getTitle());
        assertFalse(product.isAvailable());
    }

    @Test
    @DisplayName("deleteProduct: если продукт не найден, выбросить NoSuchElementException")
    public void deleteProduct_UUidIsInvalid_ThrowNoSuchElementException() {
        //given
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        //when
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> productService.deleteProduct(productId)
        );

        //then
        assertEquals("catalogue.errors.product.not_found", exception.getMessage());
    }

    @Test
    @DisplayName("findProductById: если товар не найден, то выбросить NoSuchElementException")
    public void findProductById_UUidIsInvalid_ThrowNoSuchElementException() {
        //given
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        //when
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> productService.findProductById(productId)
        );

        //then
        assertEquals("catalogue.errors.product.not_found", exception.getMessage());
    }

    @Test
    @DisplayName("findProductById: если товар найден, то вернуть его")
    public void findProductById_UUidIsValid_ReturnProduct() {
        //given
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(
                        Product.builder()
                                .id(productId)
                                .title("TestProduct")
                                .build()
                )
        );

        //when
        ProductDto result = productService.findProductById(productId);

        //then
        assertNotNull(result);
        assertEquals(productId, result.id());
        assertEquals("TestProduct", result.title());
    }


    @Test
    @DisplayName("updateProduct: успешное обновление полей и замена существующей картинки в MinIO")
    void updateProduct_WithNewImageAndExistingOldImage_ShouldUpdateFieldsUploadNewAndDeleteOldImage() {
        // given
        UUID productId = UUID.randomUUID();
        String oldImage = "old-uuid-image.png";
        String newImageName = "new-uuid-image.png";

        Product existingProduct = Product.builder()
                .id(productId)
                .title("Старое название")
                .description("Старое описание")
                .price(new BigDecimal("100.00"))
                .imageFileName(oldImage)
                .build();

        MockMultipartFile newImageFile = new MockMultipartFile(
                "image", "new.png", "image/png", "new-bytes".getBytes()
        );

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(defaultMinioService.upload(newImageFile)).thenReturn(newImageName);

        // when
        productService.updateProduct(
                productId,
                "Новое название",
                "Новое описание",
                1,
                new BigDecimal("200.00"),
                newImageFile
        );

        // then
        assertEquals("Новое название", existingProduct.getTitle());
        assertEquals("Новое описание", existingProduct.getDescription());
        assertEquals(new BigDecimal("200.00"), existingProduct.getPrice());
        assertEquals(newImageName, existingProduct.getImageFileName());

        verify(productRepository, times(1)).findById(productId);
        verify(defaultMinioService, times(1)).upload(newImageFile);
        verify(defaultMinioService, times(1)).removeObject(oldImage);
    }

    @Test
    @DisplayName("updateProduct: обновление без передачи файла изображения (поля обновляются, MinIO не вызывается)")
    void updateProduct_WithoutImage_ShouldOnlyUpdateProductFields() {
        // given
        UUID productId = UUID.randomUUID();
        String oldImage = "old-image.png";

        Product existingProduct = Product.builder()
                .id(productId)
                .title("Старое")
                .description("Старое")
                .price(BigDecimal.TEN)
                .imageFileName(oldImage)
                .build();

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));

        // when
        productService.updateProduct(
                productId,
                "Обновленное",
                "Обновленное",
                1,
                BigDecimal.ONE,
                null // Картинка не передана
        );

        // then
        assertEquals("Обновленное", existingProduct.getTitle());
        assertEquals(oldImage, existingProduct.getImageFileName()); // Картинка осталась прежней

        verify(defaultMinioService, never()).upload(any());
        verify(defaultMinioService, never()).removeObject(anyString());
    }

    @Test
    @DisplayName("updateProduct: загрузка картинки, когда у товара раньше НЕ было картинки (removeObject не вызывается)")
    void updateProduct_WithNewImageWhenOldImageWasNull_ShouldUploadNewAndNotDeleteOld() {
        // given
        UUID productId = UUID.randomUUID();
        Product existingProduct = Product.builder()
                .id(productId)
                .imageFileName(null) // Старой картинки не было
                .build();

        MockMultipartFile newImageFile = new MockMultipartFile(
                "image", "avatar.png", "image/png", "bytes".getBytes()
        );
        String newImageName = "new-uuid.png";

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(defaultMinioService.upload(newImageFile)).thenReturn(newImageName);

        // when
        productService.updateProduct(productId, "Title", "Desc", 1, BigDecimal.TEN, newImageFile);

        // then
        assertEquals(newImageName, existingProduct.getImageFileName());
        verify(defaultMinioService, times(1)).upload(newImageFile);
        verify(defaultMinioService, never()).removeObject(anyString());
    }

    @Test
    @DisplayName("updateProduct: выбрасывает NoSuchElementException, если продукт не найден")
    void updateProduct_ProductNotFound_ShouldThrowNoSuchElementException() {
        // given
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        // when & then
        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> productService.updateProduct(productId, "T", "D", 1, BigDecimal.ONE, null)
        );

        assertEquals("catalogue.errors.product.not_found", exception.getMessage());
        verify(defaultMinioService, never()).upload(any());
    }

    @Test
    @DisplayName("updateProduct: при ошибке в minioService.removeObject метод НЕ должен падать")
    void updateProduct_MinioRemoveThrowsException_ShouldCatchAndCompleteSuccessfully() {
        // given
        UUID productId = UUID.randomUUID();
        String oldImage = "old.png";
        Product existingProduct = Product.builder()
                .id(productId)
                .imageFileName(oldImage)
                .build();

        MockMultipartFile newImageFile = new MockMultipartFile("image", "new.png", "image/png", "b".getBytes());

        when(productRepository.findById(productId)).thenReturn(Optional.of(existingProduct));
        when(defaultMinioService.upload(newImageFile)).thenReturn("new-uuid.png");

        // Имитируем падение MinIO при удалении
        doThrow(new RuntimeException("MinIO error"))
                .when(defaultMinioService).removeObject(oldImage);

        // when & then — проверяем, что вызов метода не падает
        assertDoesNotThrow(() -> productService.updateProduct(
                productId, "Title", "Desc", 1, BigDecimal.TEN, newImageFile
        ));

        verify(defaultMinioService, times(1)).removeObject(oldImage);
    }

    @Test
    @DisplayName("deductStock: списывает товар и помечает заказ обработанным")
    void deductStock_WithSufficientStock_ShouldDeductAndMarkProcessed() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).quantity(5).build();

        when(processedOrderEventRepository.existsById(orderId)).thenReturn(false);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        // when
        productService.deductStock(orderId, List.of(new OrderItemPayload(productId, 3)));

        // then
        assertEquals(2, product.getQuantity());
        ArgumentCaptor<ProcessedOrderEvent> eventCaptor = ArgumentCaptor.forClass(ProcessedOrderEvent.class);
        verify(processedOrderEventRepository, times(1)).save(eventCaptor.capture());
        assertEquals(orderId, eventCaptor.getValue().getOrderId());
    }

    @Test
    @DisplayName("deductStock: если товара не хватает, выбрасывает исключение и не помечает заказ обработанным")
    void deductStock_WithInsufficientStock_ShouldThrowAndNotMarkProcessed() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).quantity(1).build();

        when(processedOrderEventRepository.existsById(orderId)).thenReturn(false);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        // when & then
        assertThrows(InsufficientStockException.class, () ->
                productService.deductStock(orderId, List.of(new OrderItemPayload(productId, 3)))
        );

        assertEquals(1, product.getQuantity());
        verify(processedOrderEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("deductStock: если заказ уже был обработан ранее (повтор из Kafka), товар повторно не списывается")
    void deductStock_WhenOrderAlreadyProcessed_ShouldSkipSilently() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(processedOrderEventRepository.existsById(orderId)).thenReturn(true);

        // when
        productService.deductStock(orderId, List.of(new OrderItemPayload(productId, 3)));

        // then
        verify(productRepository, never()).findByIdForUpdate(any());
        verify(processedOrderEventRepository, never()).save(any());
    }

    @Test
    @DisplayName("restoreStock: если заказ был обработан (сток списан), возвращает товар на склад и снимает пометку обработанности")
    void restoreStock_WhenOrderWasProcessed_ShouldRestoreQuantityAndUnmarkProcessed() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Product product = Product.builder().id(productId).quantity(2).build();

        when(processedOrderEventRepository.existsById(orderId)).thenReturn(true);
        when(productRepository.findByIdForUpdate(productId)).thenReturn(Optional.of(product));

        // when
        productService.restoreStock(orderId, List.of(new OrderItemPayload(productId, 3)));

        // then
        assertEquals(5, product.getQuantity());
        verify(processedOrderEventRepository, times(1)).deleteById(orderId);
    }

    @Test
    @DisplayName("restoreStock: если заказ не был обработан (сток никогда не списывался), ничего не меняет")
    void restoreStock_WhenOrderWasNotProcessed_ShouldDoNothing() {
        // given
        UUID orderId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        when(processedOrderEventRepository.existsById(orderId)).thenReturn(false);

        // when
        productService.restoreStock(orderId, List.of(new OrderItemPayload(productId, 3)));

        // then
        verify(productRepository, never()).findByIdForUpdate(any());
        verify(processedOrderEventRepository, never()).deleteById(any());
    }
}