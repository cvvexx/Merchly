package io.cvvexxx.apigateway.client;

import io.cvvexxx.apigateway.dto.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@Slf4j
@RequiredArgsConstructor
public class KeycloakRestClient {

    public static final String GRANT_TYPE = "password";
    public static final String REFRESH_TOKEN = "refresh_token";

    private final RestClient restClient;
    private final String clientId;
    private final String tokenUri;
    private final String clientSecret;
    private final String logoutUri;

    public KeycloakTokenResponse login(String username, String password) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("grant_type", GRANT_TYPE);
        formData.add("username", username);
        formData.add("password", password);
        formData.add("client_secret", clientSecret);

        return restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KeycloakTokenResponse.class);
    }

    public KeycloakTokenResponse refresh(String refreshToken) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("grant_type", REFRESH_TOKEN);
        formData.add("refresh_token", refreshToken);
        formData.add("client_secret", clientSecret);

        return restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(formData)
                .retrieve()
                .body(KeycloakTokenResponse.class);
    }

    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("client_id", clientId);
        formData.add("client_secret", clientSecret);
        formData.add("refresh_token", refreshToken);

        try {
            restClient.post()
                    .uri(logoutUri)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();
        } catch (Exception e) {
            log.warn("Не удалось отозвать refresh-токен в Keycloak: {}", e.getMessage());
        }
    }
}
