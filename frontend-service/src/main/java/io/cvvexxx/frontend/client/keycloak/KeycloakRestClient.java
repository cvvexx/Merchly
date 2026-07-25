package io.cvvexxx.frontend.client.keycloak;

import io.cvvexxx.frontend.dto.KeycloakTokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class KeycloakRestClient {

    public static final String GRANT_TYPE = "password";

    private final RestClient restClient;
    private final String clientId;
    private final String tokenUri;
    private final String clientSecret;

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
}
