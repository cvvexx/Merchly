package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.user.CreatedUserDto;
import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.dto.user.UpdateUserDto;
import io.cvvexxx.frontend.dto.user.UserInfoDto;
import io.cvvexxx.frontend.utils.MultipartBodyBuilderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

@RequiredArgsConstructor
@Slf4j
public class RestClientUserPublicRestClient implements UserPublicRestClient {

    private final RestClient restClient;
    private final MultipartBodyBuilderUtils builderUtils = new MultipartBodyBuilderUtils();

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
        var builder = builderUtils.multipartBodyBuilder(newUserDto, userAvatar);

        return restClient
                .post()
                .uri("/api/users/register")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .body(CreatedUserDto.class);
    }

    @Override
    public void updateUserInfo(UpdateUserDto updateUserDto, MultipartFile userAvatar) {
        var builder = builderUtils.multipartBodyBuilder(updateUserDto, userAvatar);

        restClient
                .post()
                .uri("/api/users/edit")
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .body(builder.build())
                .retrieve()
                .toBodilessEntity();
    }
}
