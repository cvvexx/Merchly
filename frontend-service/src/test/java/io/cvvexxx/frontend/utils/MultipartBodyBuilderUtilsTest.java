package io.cvvexxx.frontend.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.MultiValueMap;

import static org.junit.jupiter.api.Assertions.*;

class MultipartBodyBuilderUtilsTest {

    private MultipartBodyBuilderUtils builderUtils;

    @BeforeEach
    void setUp() {
        // given
        builderUtils = new MultipartBodyBuilderUtils();
    }

    private record SampleDto(String name, int price) {
    }

    @Nested
    @DisplayName("Тесты метода multipartBodyBuilder")
    class MultipartBodyBuilderTests {

        @Test
        @DisplayName("Добавляет только 'payload', если файл равен null")
        void multipartBodyBuilder_NullFile_AddsOnlyPayload() {
            // given
            SampleDto dto = new SampleDto("Item", 100);

            // when
            MultipartBodyBuilder builder = builderUtils.multipartBodyBuilder(dto, null);
            MultiValueMap<String, HttpEntity<?>> body = builder.build();

            // then
            assertTrue(body.containsKey("payload"));
            assertFalse(body.containsKey("image"));

            HttpEntity<?> payloadPart = body.getFirst("payload");
            assertNotNull(payloadPart);
            assertEquals(MediaType.APPLICATION_JSON, payloadPart.getHeaders().getContentType());
            assertEquals(dto, payloadPart.getBody());
        }

        @Test
        @DisplayName("Добавляет только 'payload', если файл пустой (isEmpty() == true)")
        void multipartBodyBuilder_EmptyFile_AddsOnlyPayload() {
            // given
            SampleDto dto = new SampleDto("Item", 100);
            MockMultipartFile emptyFile = new MockMultipartFile("image", "", "image/png", new byte[0]);

            // when
            MultipartBodyBuilder builder = builderUtils.multipartBodyBuilder(dto, emptyFile);
            MultiValueMap<String, HttpEntity<?>> body = builder.build();

            // then
            assertTrue(body.containsKey("payload"));
            assertFalse(body.containsKey("image"));
        }

        @Test
        @DisplayName("Добавляет и 'payload', и 'image', если передан непустой файл")
        void multipartBodyBuilder_ValidFile_AddsBothParts() {
            // given
            SampleDto dto = new SampleDto("Item", 100);
            MockMultipartFile validFile = new MockMultipartFile(
                    "image",
                    "photo.png",
                    "image/png",
                    "test content".getBytes()
            );

            // when
            MultipartBodyBuilder builder = builderUtils.multipartBodyBuilder(dto, validFile);
            MultiValueMap<String, HttpEntity<?>> body = builder.build();

            // then
            assertTrue(body.containsKey("payload"));
            assertTrue(body.containsKey("image"));

            HttpEntity<?> imagePart = body.getFirst("image");
            assertNotNull(imagePart);
            assertEquals(validFile.getResource(), imagePart.getBody());
        }
    }
}