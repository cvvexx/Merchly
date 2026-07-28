package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.CreatedUserDto;
import io.cvvexxx.frontend.dto.NewUserDto;
import io.cvvexxx.frontend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
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
    public CreatedUserDto registerUser(NewUserDto newUserDto, MultipartFile userAvatar) {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        builder.part("payload", newUserDto, MediaType.APPLICATION_JSON);
        log.info("image {}", userAvatar);
        if (userAvatar != null && !userAvatar.isEmpty()) {
            builder.part("image", userAvatar.getResource());
        }

        return restClient
                .post()
                .uri("/api/users/register")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(CreatedUserDto.class);
    }


}
