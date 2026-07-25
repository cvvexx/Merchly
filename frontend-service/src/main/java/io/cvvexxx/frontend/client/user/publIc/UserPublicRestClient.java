package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.CreatedUserDto;
import io.cvvexxx.frontend.dto.NewUserDto;
import io.cvvexxx.frontend.dto.UserInfoDto;

public interface UserPublicRestClient {

    UserInfoDto getUserInfo();

    CreatedUserDto registerUser(NewUserDto newUserDto);
}
