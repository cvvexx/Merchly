package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.user.CreatedUserDto;
import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.dto.user.UpdateUserDto;
import io.cvvexxx.frontend.dto.user.UserInfoDto;
import org.springframework.web.multipart.MultipartFile;

public interface UserPublicRestClient {

    UserInfoDto getUserInfo();

    CreatedUserDto registerUser(NewUserDto newUserDto, MultipartFile userAvatar);

    void updateUserInfo(UpdateUserDto updateUserDto, MultipartFile userAvatar);
}
