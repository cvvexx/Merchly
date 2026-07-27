package io.cvvexxx.frontend.dto;

import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(
        int id,
        String title,
        String description,
        BigDecimal price,
        String imageFileName,
        UUID createdBy
) {

    public String getImageUrl() {
        if (this.imageFileName == null || this.imageFileName.isBlank()) {
            return "/images/default-product-image.png";
        }
        return "http://localhost:9000/merchly-products/" + this.imageFileName;
    }

}
