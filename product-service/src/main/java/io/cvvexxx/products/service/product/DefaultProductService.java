package io.cvvexxx.products.service.product;

import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.event.OrderItemPayload;
import io.cvvexxx.products.exception.InsufficientStockException;
import io.cvvexxx.products.repository.ProductRepository;
import io.cvvexxx.products.service.minio.DefaultMinioService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class DefaultProductService implements ProductService {

    public static final String CACHE_PRODUCTS_LIST_NAME = "productList";
    public static final String CACHE_PRODUCT_NAME = "product";
    private final ProductRepository productRepository;
    private final DefaultMinioService defaultMinioService;

    @Override
    @Cacheable(value = CACHE_PRODUCTS_LIST_NAME, key = "#filter != null ? #filter : 'ALL'")
    @Transactional
    public List<ProductDto> findAllProducts(String filter) {
        log.info("findAllProducts filter: {}", filter);
        if (filter != null && !filter.isBlank()) {
            return productRepository.findAllByTitleContainingIgnoreCase(filter);
        } else {
            return productRepository.findAll().stream()
                    .sorted(Comparator.comparing(Product::getCreatedAt))
                    .map(this::mapToDto)
                    .toList();
        }
    }

    @Override
    @Cacheable(value = CACHE_PRODUCT_NAME, key = "#productId", unless = "#result == null")
    public ProductDto findProductById(UUID productId) {
        log.info("Find product by id {}", productId);
        var product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));
        return mapToDto(product);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_PRODUCTS_LIST_NAME, allEntries = true)
    public ProductDto createProduct(
            String title, String description, Integer quantity,
            BigDecimal price, UUID createdBy, MultipartFile image
    ) {
        String imageFileName = null;
        log.info("image {}", image);
        if (image != null && !image.isEmpty()) {
            imageFileName = defaultMinioService.upload(image);
            log.info("imageFileName: {}", imageFileName);
        }

        return mapToDto(
                productRepository.save(
                        Product.builder()
                                .id(UUID.randomUUID())
                                .title(title)
                                .description(description)
                                .quantity(quantity)
                                .price(price)
                                .createdBy(createdBy)
                                .imageFileName(imageFileName)
                                .build()
                )
        );
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCT_NAME, key = "#productId"),
            @CacheEvict(value = CACHE_PRODUCTS_LIST_NAME, allEntries = true)
    })
    public void deleteProduct(UUID productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));

        product.setAvailable(false);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCT_NAME, key = "#productId"),
            @CacheEvict(value = CACHE_PRODUCTS_LIST_NAME, allEntries = true)
    })
    public void updateProduct(UUID productId, String title, String description, Integer quantity, BigDecimal price, MultipartFile image) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));//TODO(СДЕЛАТЬ КАСТОМНЫЕ EXC)

        log.info("update product {}", product);

        String oldImageFileName = product.getImageFileName();
        log.info("New image {}", image);
        log.info("oldImageFileName {}", oldImageFileName);
        if (image != null && !image.isEmpty()) {
            String newImageFileName = defaultMinioService.upload(image);
            log.info("Uploaded new image: {}", newImageFileName);

            product.setImageFileName(newImageFileName);

            if (oldImageFileName != null && !oldImageFileName.isBlank()) {
                try {
                    defaultMinioService.removeObject(oldImageFileName);
                } catch (Exception e) {
                    log.error("Failed to delete old image {} from MinIO for product {}", oldImageFileName, productId, e);
                }
            }
        }
        product.setTitle(title);
        product.setDescription(description);
        product.setQuantity(quantity);
        product.setPrice(price);
    }

    @Override
    @Transactional(readOnly = true)
    @CacheEvict(value = CACHE_PRODUCTS_LIST_NAME, allEntries = true, key = "#ids")
    public List<ProductDto> findAllByIdIn(List<UUID> ids) {
        return productRepository.findAllById(ids).stream().map(this::mapToDto)
                .toList();
    }

    @Transactional
    @Override
    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCT_NAME, allEntries = true),
            @CacheEvict(value = CACHE_PRODUCTS_LIST_NAME, allEntries = true)
    })
    public void deductStock(List<OrderItemPayload> items) {
        log.info("checking if order is correct. Items {}", items);
        for (OrderItemPayload item : items) {
            Product product = productRepository.findById(item.productId())
                    .orElseThrow(() -> new EntityNotFoundException("Товар не найден: " + item.productId()));
            log.info("product quantity: {}. Order quantity {}", product.getQuantity(), item.quantity());
            if (product.getQuantity() < item.quantity()) {
                throw new InsufficientStockException(product.getId(),
                        "Недостаточно товара на складе ID: " + product.getId());
            }

            product.setQuantity(product.getQuantity() - item.quantity());
        }
    }

    private ProductDto mapToDto(Product product) {
        return new ProductDto(
                product.getId(),
                product.getTitle(),
                product.getDescription(),
                product.getQuantity(),
                product.getPrice(),
                product.getImageFileName(),
                product.getCreatedBy());
    }
}