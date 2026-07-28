package io.cvvexxx.frontend.client.product;

import io.cvvexxx.frontend.dto.Product;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductsRestClient {

    List<Product> findAllProducts(String filter);

    Optional<Product> findProductById(int productId);

    Product createProduct(
            String title,
            String description,
            BigDecimal price,
            MultipartFile image,
            UUID createdBy
    );

    void deleteProduct(int productId);

    void updateProduct(int productId, String title, String description, BigDecimal price, MultipartFile image);

}
