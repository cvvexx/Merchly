package io.cvvexxx.products.service;

import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
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
    private final MinioService minioService;

    @Override
    @Cacheable(value = CACHE_PRODUCTS_LIST_NAME, key = "#filter != null ? #filter : 'ALL'")
    @Transactional
    public List<Product> findAllProducts(String filter) {
        log.info("findAllProducts filter: {}", filter);
        if (filter != null && !filter.isBlank()) {
            return productRepository.findAllByTitleContainingIgnoreCase(filter);
        } else {
            return productRepository.findAll().stream().sorted(Comparator.comparing(Product::getId)).toList();
        }
    }

    @Override
    @Cacheable(value = CACHE_PRODUCT_NAME, key = "#productId", unless = "#result == null")
    public Product findProductById(int productId) {
        log.info("Find product by id {}", productId);
        return productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_PRODUCTS_LIST_NAME, allEntries = true)
    public Product createProduct(
            String title, String description, BigDecimal price,
            UUID createdBy, MultipartFile image
    ) {
        String imageFileName = null;
        log.info("image {}", image);
        if (image != null && !image.isEmpty()) {
            imageFileName = minioService.upload(image);
            log.info("imageFileName: {}", imageFileName);
        }

        return productRepository.save(
                Product.builder()
                        .title(title)
                        .description(description)
                        .price(price)
                        .createdBy(createdBy)
                        .imageFileName(imageFileName)
                        .build()
        );
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = CACHE_PRODUCT_NAME, key = "#productId"),
            @CacheEvict(value = CACHE_PRODUCTS_LIST_NAME, allEntries = true)
    })
    public void deleteProduct(Integer productId) {
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
    public void updateProduct(Integer productId, String title, String description, BigDecimal price) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));

        product.setTitle(title);
        product.setDescription(description);
        product.setPrice(price);
    }
}