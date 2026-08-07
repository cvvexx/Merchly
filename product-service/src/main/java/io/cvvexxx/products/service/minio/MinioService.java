package io.cvvexxx.products.service.minio;

import org.springframework.web.multipart.MultipartFile;

public interface MinioService {

    String upload(MultipartFile file);

    void removeObject(String fileName);
}