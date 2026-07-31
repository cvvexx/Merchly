package io.cvvexxx.frontend.config;

import io.cvvexxx.frontend.client.keycloak.KeycloakRestClient;
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
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

import java.io.IOException;

@Configuration
public class RestClientBeans {

    @Bean
    public OAuth2AuthorizedClientManager authorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        var authorizedClientProvider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode()
                .refreshToken()
                .clientCredentials() // Включаем поддержку client_credentials
                .build();

        var authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository
        );
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
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

    private ClientHttpResponse getClientHttpRequestInterceptor(
            HttpRequest request,
            byte[] body,
            ClientHttpRequestExecution execution,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) throws IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 1. Вариант, если аутентификация через OAuth2 (SSO/Redirect)
        if (authentication instanceof OAuth2AuthenticationToken oauth2Token) {
            OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                    .withClientRegistrationId(oauth2Token.getAuthorizedClientRegistrationId())
                    .principal(authentication)
                    .build();

            OAuth2AuthorizedClient client = authorizedClientManager.authorize(authorizeRequest);

            if (client != null && client.getAccessToken() != null) {
                request.getHeaders().add(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + client.getAccessToken().getTokenValue()
                );
            }
        } else if (authentication instanceof KeycloakJwtAuthenticationToken jwtToken) {
            if (jwtToken.getCredentials() != null) {
                request.getHeaders().add(
                        HttpHeaders.AUTHORIZATION,
                        "Bearer " + jwtToken.getCredentials()
                );
            }
        }

        return execution.execute(request, body);
    }

    @Bean
    public RestClientProductsPublicRestClient productsPublicRestClient(
            @Value("${spring.restclient.uri.product_service:http://localhost:8081}") String restClientUri,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return new RestClientProductsPublicRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor((request, body, execution) ->
                                getClientHttpRequestInterceptor(request, body, execution, authorizedClientManager)
                        )
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
            @Value("${spring.restclient.uri.user_service:http://localhost:8082}") String restClientUri,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return new RestClientUserPublicRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor((request, body, execution) ->
                                getClientHttpRequestInterceptor(request, body, execution, authorizedClientManager)
                        )
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
            @Value("${spring.restclient.uri.reviews_service}") String restClientUri,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        return new RestClientReviewsRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor((request, body, execution) ->
                                getClientHttpRequestInterceptor(request, body, execution, authorizedClientManager)
                        )
                        .build()
        );
    }

    @Bean
    public KeycloakRestClient keycloakAuthClient(
            @Value("${keycloak.client-id:merchly-frontend-client}")
            String clientId,

            @Value("${keycloak.token-uri:http://localhost:8090/realms/merchly/protocol/openid-connect/token}")
            String tokenUri,

            @Value("${spring.security.oauth2.client.registration.keycloak.client-secret}")
            String clientSecret
    ) {
        return new KeycloakRestClient(
                RestClient.builder().build(),
                clientId,
                tokenUri,
                clientSecret
        );
    }
}