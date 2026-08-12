package io.cvvexxx.orders.config;

import io.cvvexxx.orders.client.RestClientProductsRestClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientBeans {

    @Bean
    public RestClientProductsRestClient restClientProductsRestClient(
            @Value("${spring.restclient.uri.product_service:http://localhost:8081}") String restClientUri,
            @Qualifier("serviceAccountAuthorizedClientManager")
            OAuth2AuthorizedClientManager serviceAccountAuthorizedClientManager
    ) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(serviceAccountAuthorizedClientManager);

        interceptor.setClientRegistrationIdResolver(request -> "internal-service-client");

        return new RestClientProductsRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(interceptor)
                        .build()
        );
    }

    @Bean("serviceAccountAuthorizedClientManager")
    public OAuth2AuthorizedClientManager serviceAccountAuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService
    ) {
        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .clientCredentials()
                .build();

        var authorizedClientManager = new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientService
        );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

}
