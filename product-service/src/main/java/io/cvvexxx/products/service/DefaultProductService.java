package io.cvvexxx.products.service;


import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultProductService implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> findAllProducts(
            String filter
    ) {
        if (filter != null && !filter.isBlank()) {
            return productRepository.findAllByTitleLikeIgnoreCase(filter);
        } else {
            return productRepository.findAll();
        }
    }

    @Override
    public Optional<Product> findProductById(int productId) {
        return productRepository.findById(productId);
    }

    @Override
    @Transactional
    public Product createProduct(String title, String description, BigDecimal price, int createdBy) {
        return productRepository.save(
                Product.builder()
                        .title(title)
                        .description(description)
                        .price(price)
                        .createdBy(createdBy)
                        .build()
        );
    }

    @Override
    @Transactional
    public void deleteProduct(Integer productId) {
        productRepository.findById(productId)
                .ifPresentOrElse(product ->
                        product.setAvailable(false),
                        () -> new NoSuchElementException("catalogue.errors.product.not_found")
                );
    }

    @Override
    @Transactional
    public void updateProduct(Integer productId, String title, String description, BigDecimal price) {
        productRepository.findById(productId)
                .ifPresentOrElse(product -> {
                            product.setTitle(title);
                            product.setDescription(description);
                            product.setPrice(price);
                        },
                        () -> {
                            throw new NoSuchElementException();
                        }
                );
    }
}
