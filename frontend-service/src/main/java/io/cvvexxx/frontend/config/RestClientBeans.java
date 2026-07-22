package io.cvvexxx.frontend.config;

import io.cvvexxx.frontend.client.product.RestClientProductsRestClient;
import io.cvvexxx.frontend.client.user.internal.RestClientUserInternalRestClient;
import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.web.client.RestClient;
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor;

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
                .clientCredentials()
                .build();

        var authorizedClientManager = new DefaultOAuth2AuthorizedClientManager(
                clientRegistrationRepository, authorizedClientRepository
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
        }

        return execution.execute(request, body);
    }


    @Bean
    public RestClientProductsRestClient productsRestClient(
            @Value("${spring.restclient.uri.product_service:http://localhost:8081}") String restClientUri,
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {

        return new RestClientProductsRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor((request, body, execution) ->
                                getClientHttpRequestInterceptor(request, body, execution, authorizedClientManager)
                        )
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
            OAuth2AuthorizedClientManager authorizedClientManager
    ) {
        OAuth2ClientHttpRequestInterceptor interceptor =
                new OAuth2ClientHttpRequestInterceptor(authorizedClientManager);

        interceptor.setClientRegistrationIdResolver(request -> "internal-service-client");

        return new RestClientUserInternalRestClient(
                RestClient.builder()
                        .baseUrl(restClientUri)
                        .requestInterceptor(interceptor)
                        .build()
        );
    }
}