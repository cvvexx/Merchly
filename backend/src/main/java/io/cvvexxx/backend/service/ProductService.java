package io.cvvexxx.backend.service;

import io.cvvexxx.backend.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<Product> findAllProducts();

    Optional<Product> findProductById(int productId);

    Product createProduct(String title, String description);

    void deleteProduct(Integer productId);

    void updateProduct(Integer productId, String title, String description);

}
