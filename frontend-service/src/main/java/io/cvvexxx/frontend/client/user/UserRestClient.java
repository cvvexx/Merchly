package io.cvvexxx.frontend.client.user;

import io.cvvexxx.frontend.dto.JwtAuthenticationDto;
import io.cvvexxx.frontend.dto.UserDto;

public interface UserRestClient {

    JwtAuthenticationDto checkUserAuth(String username, String password);//TODO

    JwtAuthenticationDto registerUser(String username, String password);//TODO

    UserDto getUserInfo(String token);

}
