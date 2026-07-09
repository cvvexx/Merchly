package io.cvvexxx.frontend.client.user;

import io.cvvexxx.frontend.dto.UserDto;

public interface UserRestClient {

    UserDto checkUserAuth(String username, String password);

    UserDto registerUser(String username, String password);

}
