package io.cvvexxx.frontend.client.user;

import io.cvvexxx.frontend.controller.security.payload.UserLoginPayload;
import io.cvvexxx.frontend.controller.security.payload.UserRegistrationPayload;
import io.cvvexxx.frontend.dto.UserDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class RestClientUserRestClient implements UserRestClient {

    private final RestClient restClient;

    @Override
    public UserDto checkUserAuth(String username, String password) {
        return restClient
                .post()
                .uri("api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserLoginPayload(username, password))
                .retrieve()
                .body(UserDto.class);
    }

    @Override
    public UserDto registerUser(String username, String password) {
        return restClient
                .post()
                .uri("api/users/auth")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new UserRegistrationPayload(username, password))
                .retrieve()
                .body(UserDto.class);
    }

    @Override
    public UserDto getUserInfo(String token) {
        return restClient
                .get()
                .uri("api/users/me")
                .header(HttpHeaders.AUTHORIZATION, "Bearer" + token)
                .retrieve()
                .body(UserDto.class);
    }
}
