package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

@RequiredArgsConstructor
public class RestClientUserPublicRestClient implements UserPublicRestClient {

    private final RestClient restClient;


    @Override
    public UserInfoDto getUserInfo() {
        return restClient
                .get()
                .uri("api/users/me")
                .retrieve()
                .body(UserInfoDto.class);
    }


}
