package io.cvvexxx.frontend.controller.product.payload;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

public record NewProductPayload(
        String title,
        String description,
        BigDecimal price,
        UUID createdBy
) {
}
