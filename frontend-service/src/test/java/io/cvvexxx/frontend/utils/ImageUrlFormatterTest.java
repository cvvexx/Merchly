package io.cvvexxx.frontend.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ImageUrlFormatterTest {

    private final String MINIO_URL = "http://localhost:9000";
    private ImageUrlFormatter imageUrlFormatter;

    @BeforeEach
    void setUp() {
        imageUrlFormatter = new ImageUrlFormatter();
        ReflectionTestUtils.setField(imageUrlFormatter, "MINIO_URL", MINIO_URL);
    }

    @Nested
    @DisplayName("Тесты метода getProductImageUrl")
    class GetProductImageUrlTests {

        @Test
        @DisplayName("Если имя файла null, возвращает дефолтное изображение товара")
        void getProductImageUrl_NullFileName_ReturnsDefault() {
            String result = imageUrlFormatter.getProductImageUrl(null);

            assertEquals("/images/default-product-image.png", result);
        }

        @Test
        @DisplayName("Если имя файла пустое или состоит из пробелов, возвращает дефолтное изображение")
        void getProductImageUrl_BlankFileName_ReturnsDefault() {
            String result = imageUrlFormatter.getProductImageUrl("   ");

            assertEquals("/images/default-product-image.png", result);
        }

        @Test
        @DisplayName("Если передано корректное имя файла, возвращает полный MinIO URL")
        void getProductImageUrl_ValidFileName_ReturnsFormattedUrl() {
            String result = imageUrlFormatter.getProductImageUrl("item123.jpg");

            assertEquals("http://localhost:9000/merchly-products/item123.jpg", result);
        }
    }

    @Nested
    @DisplayName("Тесты метода getUserAvatarUrl")
    class GetUserAvatarUrlTests {

        @Test
        @DisplayName("Если имя файла null, возвращает дефолтный аватар пользователя")
        void getUserAvatarUrl_NullFileName_ReturnsDefault() {
            String result = imageUrlFormatter.getUserAvatarUrl(null);

            assertEquals("/images/default-user-avatar.png", result);
        }

        @Test
        @DisplayName("Если имя файла пустое, возвращает дефолтный аватар")
        void getUserAvatarUrl_BlankFileName_ReturnsDefault() {
            String result = imageUrlFormatter.getUserAvatarUrl("");

            assertEquals("/images/default-user-avatar.png", result);
        }

        @Test
        @DisplayName("Если передано корректное имя файла, возвращает полный MinIO URL аватара")
        void getUserAvatarUrl_ValidFileName_ReturnsFormattedUrl() {
            String result = imageUrlFormatter.getUserAvatarUrl("avatar.png");

            assertEquals("http://localhost:9000/merchly-users/avatar.png", result);
        }
    }
}
