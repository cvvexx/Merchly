package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.product.AddToCartDto;
import io.cvvexxx.frontend.dto.product.CartItemDto;
import io.cvvexxx.frontend.dto.user.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface UserPublicRestClient {

    UserInfoDto getUserInfo();

    CreatedUserDto registerUser(NewUserDto newUserDto, MultipartFile userAvatar);

    void updateUserInfo(UpdateUserDto updateUserDto, MultipartFile userAvatar);

    void addProductToCart(AddToCartDto addToCartDto);

    List<CartItemDto> getCartItems();

    void deleteProductFromCart(UUID productId);

    UserProfilePublicDto getUserProfile(String username);

    void getAdminRole();
}
