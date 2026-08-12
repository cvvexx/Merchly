package io.cvvexxx.users.service.user;

import io.cvvexxx.users.dto.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserService {

    UserCreatedDto registerUserInKeycloakAndLocalDb(NewUserDto newUserDto, MultipartFile userAvatar);

    UserInfoDto getUserInfo(UUID userId);

    List<UserProductOwnerDto> findUsersByIds(List<UUID> ids);

    UserProductOwnerDto findUserById(UUID userId);

    void updateUserInfo(UUID userId, UpdateUserDto updateUserDto, MultipartFile userAvatar);

    UserProfilePublicDto getPublicUserProfile(String username);

}