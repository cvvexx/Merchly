package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.CreatedUserDto;
import io.cvvexxx.frontend.dto.NewUserDto;
import io.cvvexxx.frontend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
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

    @Override
    public CreatedUserDto registerUser(NewUserDto newUserDto) {
        return restClient
                .post()
                .uri("/api/users/register")
                .body(newUserDto)
                .retrieve()
                .body(CreatedUserDto.class);
    }


}
