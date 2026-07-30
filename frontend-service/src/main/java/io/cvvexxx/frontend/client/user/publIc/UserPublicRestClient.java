package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.product.AddToCartDto;
import io.cvvexxx.frontend.dto.product.CartItemDto;
import io.cvvexxx.frontend.dto.user.CreatedUserDto;
import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.dto.user.UpdateUserDto;
import io.cvvexxx.frontend.dto.user.UserInfoDto;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserPublicRestClient {

    UserInfoDto getUserInfo();

    CreatedUserDto registerUser(NewUserDto newUserDto, MultipartFile userAvatar);

    void updateUserInfo(UpdateUserDto updateUserDto, MultipartFile userAvatar);

    void addProductToCart(AddToCartDto addToCartDto);

    List<CartItemDto> getCartItems();
}
