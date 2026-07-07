package io.cvvexxx.frontend.config;

import io.cvvexxx.frontend.client.product.RestClientProductsRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientBeans {

    @Bean
    public RestClientProductsRestClient productsRestClient(
        @Value("${spring.restclient.uri.product_service:http://localhost:8081}") String restClientUri
    ) {
        return new RestClientProductsRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .build()
        );
    }

}
