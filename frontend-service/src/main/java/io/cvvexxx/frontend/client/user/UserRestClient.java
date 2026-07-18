package io.cvvexxx.frontend.client.user;

import io.cvvexxx.frontend.controller.security.payload.UserLoginPayload;
import io.cvvexxx.frontend.controller.security.payload.NewUserPayload;
import io.cvvexxx.frontend.dto.ProductOwnerDto;
import io.cvvexxx.frontend.dto.UserInfoDto;
import io.cvvexxx.frontend.dto.JwtAuthenticationDto;

import java.util.List;

public interface UserRestClient {

    JwtAuthenticationDto checkUserAuth(UserLoginPayload payload);

    JwtAuthenticationDto registerUser(NewUserPayload payload);

    UserInfoDto getUserInfo(String token);

    JwtAuthenticationDto refreshTokens(String refreshToken);

    List<ProductOwnerDto> findAllUsersByIds(List<Integer> userIds);

    ProductOwnerDto findUserById(Integer userId);
}
