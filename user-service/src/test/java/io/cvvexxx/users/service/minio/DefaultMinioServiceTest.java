package io.cvvexxx.users.service.minio;

import io.minio.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMinioServiceTest {

    private static final String BUCKET = "users-bucket";

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private DefaultMinioService minioService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(minioService, "bucketName", BUCKET);
    }

    @Nested
    @DisplayName("Тесты метода upload")
    class UploadTests {

        @Test
        @DisplayName("upload: если файл пустой, выбрасывает IllegalArgumentException")
        void upload_WhenFileIsEmpty_ShouldThrowIllegalArgumentException() {
            // given
            MultipartFile emptyFile = new MockMultipartFile("image", "avatar.png", "image/png", new byte[0]);

            // when / then
            assertThrows(IllegalArgumentException.class, () -> minioService.upload(emptyFile));
            verifyNoInteractions(minioClient);
        }

        @Test
        @DisplayName("upload: если оригинальное имя файла отсутствует, выбрасывает IllegalArgumentException")
        void upload_WhenOriginalFilenameIsNull_ShouldThrowIllegalArgumentException() {
            // given
            MultipartFile fileWithoutName = org.mockito.Mockito.mock(MultipartFile.class);
            when(fileWithoutName.isEmpty()).thenReturn(false);
            when(fileWithoutName.getOriginalFilename()).thenReturn(null);

            // when / then
            assertThrows(IllegalArgumentException.class, () -> minioService.upload(fileWithoutName));
            verifyNoInteractions(minioClient);
        }

        @Test
        @DisplayName("upload: если бакет не существует, создает его перед загрузкой файла")
        void upload_WhenBucketDoesNotExist_ShouldCreateBucketAndUploadFile() throws Exception {
            // given
            MultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", new byte[]{1, 2, 3});
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

            // when
            String fileName = minioService.upload(file);

            // then
            assertTrue(fileName.endsWith(".avatar.png"));
            verify(minioClient).makeBucket(any(MakeBucketArgs.class));
            verify(minioClient).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("upload: если бакет уже существует, не создает его повторно")
        void upload_WhenBucketExists_ShouldNotCreateBucket() throws Exception {
            // given
            MultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", new byte[]{1, 2, 3});
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

            // when
            minioService.upload(file);

            // then
            verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
            verify(minioClient, times(1)).putObject(any(PutObjectArgs.class));
        }

        @Test
        @DisplayName("upload: если MinIO выбрасывает исключение, оборачивает его в RuntimeException")
        void upload_WhenMinioThrows_ShouldWrapInRuntimeException() throws Exception {
            // given
            MultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", new byte[]{1, 2, 3});
            when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);
            when(minioClient.putObject(any(PutObjectArgs.class))).thenThrow(new RuntimeException("MinIO down"));

            // when / then
            assertThrows(RuntimeException.class, () -> minioService.upload(file));
        }
    }

    @Nested
    @DisplayName("Тесты метода removeObject")
    class RemoveObjectTests {

        @Test
        @DisplayName("removeObject: если имя файла null, ничего не делает")
        void removeObject_WhenFileNameIsNull_ShouldDoNothing() {
            // given / when
            minioService.removeObject(null);

            // then
            verifyNoInteractions(minioClient);
        }

        @Test
        @DisplayName("removeObject: если имя файла пустое, ничего не делает")
        void removeObject_WhenFileNameIsBlank_ShouldDoNothing() {
            // given / when
            minioService.removeObject("   ");

            // then
            verifyNoInteractions(minioClient);
        }

        @Test
        @DisplayName("removeObject: удаляет файл из MinIO по имени")
        void removeObject_WhenFileNameProvided_ShouldRemoveObject() throws Exception {
            // given / when
            minioService.removeObject("avatar.png");

            // then
            verify(minioClient).removeObject(any(RemoveObjectArgs.class));
        }

        @Test
        @DisplayName("removeObject: если MinIO выбрасывает исключение, оборачивает его в RuntimeException")
        void removeObject_WhenMinioThrows_ShouldWrapInRuntimeException() throws Exception {
            // given
            org.mockito.Mockito.doThrow(new RuntimeException("MinIO down"))
                    .when(minioClient).removeObject(any(RemoveObjectArgs.class));

            // when
            RuntimeException ex = assertThrows(RuntimeException.class, () -> minioService.removeObject("avatar.png"));

            // then
            assertEquals("Cannot delete file from MinIO", ex.getMessage());
        }
    }
}
