package io.cvvexxx.frontend.client.product.publIc;

import io.cvvexxx.frontend.dto.product.Product;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductsPublicRestClient {

    List<Product> findAllProducts(String filter);

    Optional<Product> findProductById(UUID productId);

    Product createProduct(
            String title,
            String description,
            BigDecimal price,
            MultipartFile image,
            UUID createdBy
    );

    void deleteProduct(UUID productId);

    void updateProduct(UUID productId, String title, String description, BigDecimal price, MultipartFile image);

}
