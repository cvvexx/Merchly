package io.cvvexxx.products.service.minio;

import io.minio.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultMinioServiceTest {

    private static final String BUCKET = "merchly-products";

    @Mock
    private MinioClient minioClient;

    @InjectMocks
    private DefaultMinioService minioService;

    @Test
    @DisplayName("upload: если файл пустой, выбрасывает IllegalArgumentException и не обращается к MinIO")
    void upload_WhenFileIsEmpty_ShouldThrowIllegalArgumentException() throws Exception {
        MultipartFile emptyFile = new MockMultipartFile("image", "avatar.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class, () -> minioService.upload(emptyFile));

        verify(minioClient, never()).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("upload: если бакет не существует, создаёт его перед загрузкой файла")
    void upload_WhenBucketDoesNotExist_ShouldCreateBucketBeforeUploading() throws Exception {
        ReflectionTestUtils.setField(minioService, "bucketName", BUCKET);
        MultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", "bytes".getBytes());
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(false);

        String fileName = minioService.upload(file);

        assertTrue(fileName.endsWith("avatar.png"));
        verify(minioClient).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("upload: если бакет уже существует, не пытается создать его повторно")
    void upload_WhenBucketExists_ShouldNotCreateBucketAgain() throws Exception {
        ReflectionTestUtils.setField(minioService, "bucketName", BUCKET);
        MultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", "bytes".getBytes());
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenReturn(true);

        String fileName = minioService.upload(file);

        assertTrue(fileName.endsWith("avatar.png"));
        verify(minioClient, never()).makeBucket(any(MakeBucketArgs.class));
        verify(minioClient).putObject(any(PutObjectArgs.class));
    }

    @Test
    @DisplayName("upload: при ошибке MinIO оборачивает исключение в RuntimeException")
    void upload_WhenMinioThrows_ShouldWrapInRuntimeException() throws Exception {
        ReflectionTestUtils.setField(minioService, "bucketName", BUCKET);
        MultipartFile file = new MockMultipartFile("image", "avatar.png", "image/png", "bytes".getBytes());
        when(minioClient.bucketExists(any(BucketExistsArgs.class))).thenThrow(new RuntimeException("MinIO is down"));

        assertThrows(RuntimeException.class, () -> minioService.upload(file));
    }

    @Test
    @DisplayName("removeObject: если имя файла null или пустое, не обращается к MinIO")
    void removeObject_WhenFileNameIsBlank_ShouldNotCallMinio() throws Exception {
        String fileName = "   ";

        minioService.removeObject(fileName);

        verify(minioClient, never()).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("removeObject: если имя файла указано, удаляет объект из MinIO")
    void removeObject_WhenFileNameProvided_ShouldRemoveObjectFromMinio() throws Exception {
        ReflectionTestUtils.setField(minioService, "bucketName", BUCKET);
        String fileName = "existing-image.png";

        minioService.removeObject(fileName);

        verify(minioClient).removeObject(any(RemoveObjectArgs.class));
    }

    @Test
    @DisplayName("removeObject: при ошибке MinIO оборачивает исключение в RuntimeException")
    void removeObject_WhenMinioThrows_ShouldWrapInRuntimeException() throws Exception {
        ReflectionTestUtils.setField(minioService, "bucketName", BUCKET);
        String fileName = "existing-image.png";
        doThrow(new RuntimeException("MinIO is down")).when(minioClient).removeObject(any(RemoveObjectArgs.class));

        assertThrows(RuntimeException.class, () -> minioService.removeObject(fileName));
    }
}
