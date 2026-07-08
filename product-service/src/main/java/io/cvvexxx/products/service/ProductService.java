package io.cvvexxx.products.service;

import io.cvvexxx.products.entity.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    List<Product> findAllProducts();

    Optional<Product> findProductById(int productId);

    Product createProduct(String title, String description);

    void deleteProduct(Integer productId);

    void updateProduct(Integer productId, String title, String description);

}
