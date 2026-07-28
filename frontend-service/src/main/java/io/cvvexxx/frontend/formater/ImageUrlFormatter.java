package io.cvvexxx.frontend.formater;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlFormatter {

    @Value("${minio.url}")
    public String MINIO_URL;

    public String getImageUrl(String imageFileName) {
        if (imageFileName == null || imageFileName.isBlank()) {
            return "/images/default-product-image.png";
        }
        return "%s/merchly-products/%s".formatted(MINIO_URL, imageFileName);
    }

}
