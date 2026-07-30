package io.cvvexxx.products.service;

import io.cvvexxx.products.entity.Product;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    List<Product> findAllProducts(String filter);

    Product findProductById(UUID productId);

    Product createProduct(String title, String description, BigDecimal price, UUID createdBy, MultipartFile imageFileName);

    void deleteProduct(UUID productId);

    void updateProduct(UUID productId, String title, String description, BigDecimal price, MultipartFile imageFileName);

}
