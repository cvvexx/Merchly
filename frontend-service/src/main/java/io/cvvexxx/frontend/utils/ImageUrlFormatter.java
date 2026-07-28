package io.cvvexxx.frontend.utils;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ImageUrlFormatter {

    @Value("${minio.url}")
    public String MINIO_URL;

    private final static String PRODUCT_DEFAULT_IMAGE = "/images/default-product-image.png";
    private final static String USER_DEFAULT_AVATAR = "/images/default-user-avatar.png";

    public String getProductImageUrl(String imageFileName) {
        if (imageFileName == null || imageFileName.isBlank()) {
            return PRODUCT_DEFAULT_IMAGE;
        }
        return "%s/merchly-products/%s".formatted(MINIO_URL, imageFileName);
    }

    public String getUserAvatarUrl(String imageFileName) {
        if (imageFileName == null || imageFileName.isBlank()) {
            return USER_DEFAULT_AVATAR;
        }
        return "%s/merchly-users/%s".formatted(MINIO_URL, imageFileName);
    }
}
