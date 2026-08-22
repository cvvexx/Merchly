package io.cvvexxx.reviews.controller;

import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.dto.UpdateReviewDto;
import io.cvvexxx.reviews.service.DefaultReviewService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductsReviewControllerTest {

    @Mock
    private DefaultReviewService defaultReviewService;

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private ProductsReviewController controller;

    @Test
    @DisplayName("createReview: при ошибках валидации выбрасывает BindException и не создаёт отзыв")
    void createReview_WhenBindingResultHasErrors_ShouldThrowBindException() {
        // given
        NewReviewDto dto = new NewReviewDto(UUID.randomUUID(), 6, "invalid rating");
        var bindingResult = new BeanPropertyBindingResult(dto, "newReviewDto");
        bindingResult.reject("rating", "must be <= 5");
        Jwt jwt = jwtWithRoles(UUID.randomUUID(), List.of());

        // when / then
        assertThrows(BindException.class, () -> controller.createReview(jwt, dto, bindingResult));

        verifyNoInteractions(defaultReviewService);
    }

    private Jwt jwtWithRoles(UUID userId, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    private ReviewDto review(UUID reviewId, UUID userId) {
        return new ReviewDto(reviewId, UUID.randomUUID(), userId, 5, "great");
    }

    @Nested
    @DisplayName("определение роли ADMIN из JWT")
    class AdminRoleDetectionTests {

        @Test
        @DisplayName("realm_access.roles содержит 'ADMIN' -> isAdmin=true")
        void updateReview_WhenRealmRolesContainAdmin_ShouldPassIsAdminTrue() throws BindException {
            // given
            UUID userId = UUID.randomUUID();
            UpdateReviewDto dto = new UpdateReviewDto(UUID.randomUUID(), 5, "great");
            var bindingResult = new BeanPropertyBindingResult(dto, "updateReviewDto");
            Jwt jwt = jwtWithRoles(userId, List.of("ADMIN"));
            when(defaultReviewService.updateReview(dto, userId, true)).thenReturn(review(dto.reviewId(), userId));

            // when
            controller.updateReview(dto, jwt, bindingResult);

            // then
            verify(defaultReviewService).updateReview(dto, userId, true);
        }

        @Test
        @DisplayName("realm_access.roles без ADMIN -> isAdmin=false")
        void updateReview_WhenRealmRolesDoNotContainAdmin_ShouldPassIsAdminFalse() throws BindException {
            // given
            UUID userId = UUID.randomUUID();
            UpdateReviewDto dto = new UpdateReviewDto(UUID.randomUUID(), 5, "great");
            var bindingResult = new BeanPropertyBindingResult(dto, "updateReviewDto");
            Jwt jwt = jwtWithRoles(userId, List.of("USER"));
            when(defaultReviewService.updateReview(dto, userId, false)).thenReturn(review(dto.reviewId(), userId));

            // when
            controller.updateReview(dto, jwt, bindingResult);

            // then
            verify(defaultReviewService).updateReview(dto, userId, false);
        }

        @Test
        @DisplayName("deleteReview: извлекает userId из sub-claim и isAdmin из ролей")
        void deleteReview_ShouldExtractUserIdAndAdminFlagFromJwt() {
            // given
            UUID userId = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            Jwt jwt = jwtWithRoles(userId, List.of("ROLE_ADMIN"));

            // when
            controller.deleteReview(reviewId, jwt);

            // then
            verify(defaultReviewService).deleteReview(reviewId, userId, true);
        }
    }
}
