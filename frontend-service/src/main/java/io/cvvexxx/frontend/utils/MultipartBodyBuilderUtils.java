package io.cvvexxx.frontend.utils;

import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.multipart.MultipartFile;

public class MultipartBodyBuilderUtils {

    public MultipartBodyBuilder multipartBodyBuilder(Object dto, MultipartFile file) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("payload", dto, MediaType.APPLICATION_JSON);
        if (file != null && !file.isEmpty()) {
            builder.part("image", file.getResource());
        }
        return builder;
    }

}
