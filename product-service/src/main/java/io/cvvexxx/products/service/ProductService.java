package io.cvvexxx.products.service;

import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.entity.Product;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    List<ProductDto> findAllProducts(String filter);

    ProductDto findProductById(UUID productId);

    ProductDto createProduct(String title, String description, BigDecimal price, UUID createdBy, MultipartFile imageFileName);

    void deleteProduct(UUID productId);

    void updateProduct(UUID productId, String title, String description, BigDecimal price, MultipartFile imageFileName);

    List<ProductDto> findAllByIdIn(List<UUID> ids);
}
