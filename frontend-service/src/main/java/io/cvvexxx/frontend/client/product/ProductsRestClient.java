package io.cvvexxx.frontend.client.product;

import io.cvvexxx.frontend.entity.Product;
import java.util.List;
import java.util.Optional;

public interface ProductsRestClient {

    List<Product> findAllProducts();

    Optional<Product> findProductById(int productId);

    Product createProduct(String title, String description);

    void deleteProduct(int productId);

    void updateProduct(int productId, String title, String description);

}
