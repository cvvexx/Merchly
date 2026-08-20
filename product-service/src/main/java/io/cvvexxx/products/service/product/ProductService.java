package io.cvvexxx.products.service.product;

import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.event.OrderItemPayload;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface ProductService {

    List<ProductDto> findAllProducts(String filter);

    ProductDto findProductById(UUID productId);

    ProductDto createProduct(String title, String description, Integer quantity,
                             BigDecimal price, UUID createdBy, MultipartFile imageFileName);

    void deleteProduct(UUID productId);

    void updateProduct(UUID productId, String title, String description, Integer quantity,
                       BigDecimal price, MultipartFile imageFileName);

    List<ProductDto> findAllByIdIn(List<UUID> ids);

    void deductStock(UUID orderId, List<OrderItemPayload> items);
}
