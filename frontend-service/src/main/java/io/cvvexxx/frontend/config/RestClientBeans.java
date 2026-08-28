package io.cvvexxx.frontend.config;

import io.cvvexxx.frontend.client.order.RestClientOrdersRestClient;
import io.cvvexxx.frontend.client.product.internal.RestClientProductsInternalRestClient;
import io.cvvexxx.frontend.client.product.publIc.RestClientProductsPublicRestClient;
import io.cvvexxx.frontend.client.review.RestClientReviewsRestClient;
import io.cvvexxx.frontend.client.user.internal.RestClientUserInternalRestClient;
import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.AuthorizedClientServiceOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Configuration
public class RestClientBeans {

    private ClientHttpResponse relayUserToken(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution
    ) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication instanceof KeycloakJwtAuthenticationToken jwtToken
                && jwtToken.getAccessToken() != null) {
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + jwtToken.getAccessToken());
        }

        return execution.execute(request, body);
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

    @Bean
    public RestClientProductsPublicRestClient productsPublicRestClient(
            @Value("${spring.restclient.uri.api_gateway:${spring.restclient.uri.product_service:http://localhost:8081}}")
            String restClientUri
    ) {
        return new RestClientProductsPublicRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(this::relayUserToken)
                        .build()
        );
    }

    @Bean
    public RestClientProductsInternalRestClient productsInternalRestClient(
            @Value("${spring.restclient.uri.product_service:http://localhost:8081}") String restClientUri,
            @Qualifier("serviceAccountAuthorizedClientManager")
            OAuth2AuthorizedClientManager serviceAccountAuthorizedClientManager
    ) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(serviceAccountAuthorizedClientManager);

        interceptor.setClientRegistrationIdResolver(request -> "internal-service-client");

        return new RestClientProductsInternalRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(interceptor)
                        .build()
        );
    }

    @Bean
    public RestClientUserPublicRestClient userPublicRestClient(
            @Value("${spring.restclient.uri.api_gateway:${spring.restclient.uri.user_service:http://localhost:8082}}")
            String restClientUri
    ) {
        return new RestClientUserPublicRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(this::relayUserToken)
                        .build()
        );
    }

    @Bean
    public RestClientUserInternalRestClient userInternalRestClient(
            @Value("${spring.restclient.uri.user_service:http://localhost:8082}") String restClientUri,
            @Qualifier("serviceAccountAuthorizedClientManager")
            OAuth2AuthorizedClientManager serviceAccountAuthorizedClientManager
    ) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(serviceAccountAuthorizedClientManager);

        interceptor.setClientRegistrationIdResolver(request -> "internal-service-client");

        return new RestClientUserInternalRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(interceptor)
                        .build()
        );
    }

    @Bean
    public RestClientReviewsRestClient restClientReviewsRestClient(
            @Value("${spring.restclient.uri.api_gateway:${spring.restclient.uri.reviews_service}}") String restClientUri
    ) {
        return new RestClientReviewsRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(this::relayUserToken)
                        .build()
        );
    }

    @Bean
    public RestClientOrdersRestClient restClientOrdersRestClient(
            @Value("${spring.restclient.uri.api_gateway:${spring.restclient.uri.orders_service}}") String restClientUri
    ) {
        return new RestClientOrdersRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(this::relayUserToken)
                        .build()
        );
    }
}
