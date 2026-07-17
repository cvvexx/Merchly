package io.cvvexxx.frontend.client.user;

import io.cvvexxx.frontend.controller.security.payload.UserLoginPayload;
import io.cvvexxx.frontend.controller.security.payload.NewUserPayload;
import io.cvvexxx.frontend.dto.CurrentUserInfo;
import io.cvvexxx.frontend.dto.JwtAuthenticationDto;

public interface UserRestClient {

    JwtAuthenticationDto checkUserAuth(UserLoginPayload payload);

    JwtAuthenticationDto registerUser(NewUserPayload payload);

    CurrentUserInfo getUserInfo(String token);

    JwtAuthenticationDto refreshTokens(String refreshToken);
}
