package io.cvvexxx.frontend.client.user.publIc;

import io.cvvexxx.frontend.dto.product.AddToCartDto;
import io.cvvexxx.frontend.dto.product.CartItemDto;
import io.cvvexxx.frontend.dto.user.*;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.exception.FieldAlreadyExistsException;
import io.cvvexxx.frontend.utils.MultipartBodyBuilderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RequiredArgsConstructor
@Slf4j
public class RestClientUserPublicRestClient implements UserPublicRestClient {

    private final RestClient restClient;
    private final MultipartBodyBuilderUtils builderUtils = new MultipartBodyBuilderUtils();

    @Override
    public UserInfoDto getUserInfo() {
        return restClient
                .get()
                .uri("/api/users/me")
                .retrieve()
                .body(UserInfoDto.class);
    }

    @Override
    public CreatedUserDto registerUser(NewUserDto newUserDto, MultipartFile userAvatar) {
        try {
            var builder = builderUtils.multipartBodyBuilder(newUserDto, userAvatar);

            return restClient
                    .post()
                    .uri("/api/users/register")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(CreatedUserDto.class);
        } catch (HttpClientErrorException.BadRequest exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            throw new BadRequestException((List<String>) problemDetail.getProperties().get("errors"));
        } catch (HttpClientErrorException.Conflict exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            throw getFieldAlreadyExistsException(problemDetail);
        }
    }

    @Override
    public void updateUserInfo(UpdateUserDto updateUserDto, MultipartFile userAvatar) {
        try {
            var builder = builderUtils.multipartBodyBuilder(updateUserDto, userAvatar);

            restClient
                    .post()
                    .uri("/api/users/edit")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            throw new BadRequestException((List<String>) problemDetail.getProperties().get("errors"));
        } catch (HttpClientErrorException.Conflict exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            throw getFieldAlreadyExistsException(problemDetail);
        }
    }

    @Override
    public void addProductToCart(AddToCartDto addToCartDto) {
        restClient
                .post()
                .uri("/api/users/cart")
                .contentType(MediaType.APPLICATION_JSON)
                .body(addToCartDto)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public List<CartItemDto> getCartItems() {
        return restClient
                .get()
                .uri("/api/users/cart")
                .retrieve()
                .body(new ParameterizedTypeReference<List<CartItemDto>>() {
                });
    }

    @Override
    public void deleteProductFromCart(UUID productId) {
        restClient
                .delete()
                .uri("/api/users/cart/{productId}", productId)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public UserProfilePublicDto getUserProfile(String username) {
        return restClient
                .get()
                .uri("/api/users/{username}", username)
                .retrieve()
                .body(UserProfilePublicDto.class);
    }


    private FieldAlreadyExistsException getFieldAlreadyExistsException(ProblemDetail problemDetail) {
        String fieldName = Optional.ofNullable(problemDetail)
                .map(ProblemDetail::getProperties)
                .map(props -> (String) props.get("field"))
                .orElse("usernameOrEmail");

        String detailMessage = Optional.ofNullable(problemDetail)
                .map(ProblemDetail::getDetail)
                .orElse("Пользователь с такими данными уже существует");
        return new FieldAlreadyExistsException(fieldName, detailMessage);
    }
}
