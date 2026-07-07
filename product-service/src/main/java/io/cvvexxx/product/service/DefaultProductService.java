package io.cvvexxx.product.service;


import io.cvvexxx.product.entity.Product;
import io.cvvexxx.product.repository.ProductRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class DefaultProductService implements ProductService {

    private final ProductRepository productRepository;

    @Override
    public List<Product> findAllProducts() {
        return productRepository.findAll();
    }

    @Override
    public Optional<Product> findProductById(int productId) {
        return productRepository.findById(productId);
    }

    @Override
    @Transactional
    public Product createProduct(String title, String description) {
        return productRepository.save(new Product(null, title, description));
    }

    @Override
    @Transactional
    public void deleteProduct(Integer productId) {
        productRepository.deleteById(productId);
    }

    @Override
    @Transactional
    public void updateProduct(Integer productId, String title, String description) {
        productRepository.findById(productId)
                .ifPresentOrElse(product -> {
                            product.setTitle(title);
                            product.setDescription(description);
                        },
                        () -> {
                            throw new NoSuchElementException();
                        }
                );
    }
}
