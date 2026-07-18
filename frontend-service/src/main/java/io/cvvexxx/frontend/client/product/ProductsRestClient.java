package io.cvvexxx.frontend.client.product;

import io.cvvexxx.frontend.dto.Product;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface ProductsRestClient {

    List<Product> findAllProducts(String filter);

    Optional<Product> findProductById(int productId);

    Product createProduct(String title, String description, BigDecimal price,  Integer createdBy);

    void deleteProduct(int productId);

    void updateProduct(int productId, String title, String description, BigDecimal price);

}
