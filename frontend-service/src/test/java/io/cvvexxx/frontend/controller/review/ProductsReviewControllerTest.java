package io.cvvexxx.frontend.controller.review;

import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.dto.review.NewReviewDto;
import io.cvvexxx.frontend.dto.review.UpdateReviewDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ProductsReviewControllerTest {

    @Mock
    private ReviewsRestClient reviewsRestClient;

    @InjectMocks
    private ProductsReviewController controller;

    @Nested
    @DisplayName("createReview")
    class CreateReviewTests {

        @Test
        @DisplayName("при успешном создании отзыва делает редирект на страницу товара")
        void createReview_WhenValid_ShouldRedirectToProductPage() {
            // given
            UUID productId = UUID.randomUUID();
            NewReviewDto dto = new NewReviewDto(productId, 5, "great");
            var redirectAttributes = new RedirectAttributesModelMap();

            // when
            String result = controller.createReview(productId, dto, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            verify(reviewsRestClient).createReview(dto);
        }

        @Test
        @DisplayName("при ошибке валидации передаёт ошибки и payload как flash-атрибуты")
        void createReview_WhenBadRequest_ShouldAddFlashErrorsAndPayload() {
            // given
            UUID productId = UUID.randomUUID();
            NewReviewDto dto = new NewReviewDto(productId, 6, "invalid rating");
            var redirectAttributes = new RedirectAttributesModelMap();
            doThrow(new BadRequestException(List.of("rating must be <= 5")))
                    .when(reviewsRestClient).createReview(dto);

            // when
            String result = controller.createReview(productId, dto, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            assertEquals(List.of("rating must be <= 5"), redirectAttributes.getFlashAttributes().get("errors"));
            assertEquals(dto, redirectAttributes.getFlashAttributes().get("reviewPayload"));
        }

        @Test
        @DisplayName("при непредвиденной ошибке передаёт общее сообщение об ошибке")
        void createReview_WhenUnexpectedError_ShouldAddGenericErrorMessage() {
            // given
            UUID productId = UUID.randomUUID();
            NewReviewDto dto = new NewReviewDto(productId, 5, "great");
            var redirectAttributes = new RedirectAttributesModelMap();
            doThrow(new RuntimeException("boom")).when(reviewsRestClient).createReview(dto);

            // when
            String result = controller.createReview(productId, dto, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            assertEquals("Не удалось добавить отзыв. Попробуйте позже.",
                    redirectAttributes.getFlashAttributes().get("errorMessage"));
        }
    }

    @Nested
    @DisplayName("updateReview")
    class UpdateReviewTests {

        @Test
        @DisplayName("при успешном обновлении делает редирект на страницу товара")
        void updateReview_WhenValid_ShouldRedirectToProductPage() {
            // given
            UUID productId = UUID.randomUUID();
            UpdateReviewDto dto = new UpdateReviewDto(UUID.randomUUID(), 4, "good");
            var redirectAttributes = new RedirectAttributesModelMap();

            // when
            String result = controller.updateReview(productId, dto, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            verify(reviewsRestClient).updateReview(dto);
        }

        @Test
        @DisplayName("при ошибке валидации передаёт ошибки как flash-атрибут")
        void updateReview_WhenBadRequest_ShouldAddFlashErrors() {
            // given
            UUID productId = UUID.randomUUID();
            UpdateReviewDto dto = new UpdateReviewDto(UUID.randomUUID(), 6, "bad");
            var redirectAttributes = new RedirectAttributesModelMap();
            doThrow(new BadRequestException(List.of("error"))).when(reviewsRestClient).updateReview(dto);

            // when
            String result = controller.updateReview(productId, dto, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            assertEquals(List.of("error"), redirectAttributes.getFlashAttributes().get("errors"));
        }

        @Test
        @DisplayName("при непредвиденной ошибке передаёт общее сообщение об ошибке")
        void updateReview_WhenUnexpectedError_ShouldAddGenericErrorMessage() {
            // given
            UUID productId = UUID.randomUUID();
            UpdateReviewDto dto = new UpdateReviewDto(UUID.randomUUID(), 4, "good");
            var redirectAttributes = new RedirectAttributesModelMap();
            doThrow(new RuntimeException("boom")).when(reviewsRestClient).updateReview(dto);

            // when
            String result = controller.updateReview(productId, dto, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            assertEquals(List.of("Не удалось обновить отзыв."), redirectAttributes.getFlashAttributes().get("errors"));
        }
    }

    @Nested
    @DisplayName("deleteReview")
    class DeleteReviewTests {

        @Test
        @DisplayName("при успешном удалении делает редирект на страницу товара")
        void deleteReview_WhenValid_ShouldRedirectToProductPage() {
            // given
            UUID productId = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            var redirectAttributes = new RedirectAttributesModelMap();

            // when
            String result = controller.deleteReview(reviewId, productId, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            verify(reviewsRestClient).deleteReview(reviewId);
        }

        @Test
        @DisplayName("при ошибке удаления передаёт общее сообщение об ошибке")
        void deleteReview_WhenErrorOccurs_ShouldAddGenericErrorMessage() {
            // given
            UUID productId = UUID.randomUUID();
            UUID reviewId = UUID.randomUUID();
            var redirectAttributes = new RedirectAttributesModelMap();
            doThrow(new RuntimeException("boom")).when(reviewsRestClient).deleteReview(reviewId);

            // when
            String result = controller.deleteReview(reviewId, productId, redirectAttributes);

            // then
            assertEquals("redirect:/catalogue/products/" + productId, result);
            assertEquals("Не удалось удалить отзыв.", redirectAttributes.getFlashAttributes().get("errorMessage"));
        }
    }
}
