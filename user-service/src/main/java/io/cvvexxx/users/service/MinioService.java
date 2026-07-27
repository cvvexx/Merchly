package io.cvvexxx.users.service;

import io.minio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class MinioService {

    private final MinioClient minioClient;

    @Value("${minio.bucket.users}")
    private String bucketName;

    public String upload(MultipartFile file) {
        if (file.isEmpty() || file.getOriginalFilename() == null) {
            throw new IllegalArgumentException("Файл не может быть пустым");
        }

        String fileName = UUID.randomUUID() + "." + file.getOriginalFilename();

        try (InputStream inputStream = file.getInputStream()) {
            if (!minioClient.bucketExists(
                    BucketExistsArgs.builder()
                            .bucket(bucketName)
                            .build()
            )) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder()
                                .bucket(bucketName)
                                .build()
                );
            }
            minioClient.putObject(
                    putObjectInBucket(file, bucketName, fileName, inputStream, file.getContentType())
            );
            log.info("File successfully uploaded in MinIO: {}", fileName);
            return fileName;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void deleteAvatar(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        try {
            minioClient.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(fileName)
                            .build()
            );
            log.info("Аватар удален из MinIO: {}", fileName);
        } catch (Exception e) {
            log.error("Ошибка при удалении аватара из MinIO: {}", fileName, e);
        }
    }

    private PutObjectArgs putObjectInBucket(
            MultipartFile file,
            String bucketName,
            String fileName,
            InputStream inputStream,
            String contentType
    ) {
        return PutObjectArgs.builder()
                .bucket(bucketName)
                .object(fileName)
                .stream(inputStream, file.getSize(), -1)
                .contentType(contentType)
                .build();
    }

}
