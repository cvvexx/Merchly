package io.cvvexxx.frontend.dto;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(
        int id,
        String title,
        String description,
        BigDecimal price,
        MultipartFile image,
        UUID createdBy
) {
}
