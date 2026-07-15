package io.cvvexxx.frontend.config;

import io.cvvexxx.frontend.client.product.RestClientProductsRestClient;
import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientBeans {

    @Bean
    public RestClientProductsRestClient productsRestClient(
            @Value("${spring.restclient.uri.product_service:http://localhost:8081}") String restClientUri,
            ClientHttpRequestInterceptor tokenPropagationInterceptor
    ) {
        return new RestClientProductsRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(tokenPropagationInterceptor)
                        .build()
        );
    }

    @Bean
    public RestClientUserRestClient userRestClient(
            @Value("${spring.restclient.uri.user_service:http://localhost:8082}") String restClientUri,
            ClientHttpRequestInterceptor tokenPropagationInterceptor
    ) {
        return new RestClientUserRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(tokenPropagationInterceptor)
                        .build()
        );
    }

}