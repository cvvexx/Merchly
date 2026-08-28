package io.cvvexxx.reviews.service;

import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.dto.ReviewStatsDto;
import io.cvvexxx.reviews.dto.UpdateReviewDto;
import io.cvvexxx.reviews.entity.Review;
import io.cvvexxx.reviews.repository.ReviewRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DefaultReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private DefaultReviewService defaultReviewService;

    @Nested
    @DisplayName("Тесты метода createReview")
    class CreateReviewTest {

        @Test
        @DisplayName("createReview: если запрос корректен, то вернуть сохраненный отзыв в виде Dto")
        public void createReview_requestIsValid_returnReviewDto() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            String comment = "comment";
            int rating = 3;

            when(reviewRepository.save(any(Review.class))).thenAnswer(i -> i.getArgument(0));

            ReviewDto reviewDto = defaultReviewService.createReview(new NewReviewDto(
                    productId,
                    rating,
                    comment
            ), userId);

            assertNotNull(reviewDto);
            assertEquals("comment", reviewDto.comment());
            assertEquals(rating, reviewDto.rating());
            verify(reviewRepository, times(1)).save(any(Review.class));
        }

        @Test
        @DisplayName("createReview: если отзыв уже существует, то выбросить IllegalStateException")
        public void createReview_reviewAlreadyExists_throwIllegalStateException() {
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            String comment = "comment";
            int rating = 3;
            when(reviewRepository.existsByProductIdAndUserId(productId, userId)).thenReturn(true);

            IllegalStateException exception = assertThrows(
                    IllegalStateException.class, () -> defaultReviewService.createReview(new NewReviewDto(
                            productId,
                            rating,
                            comment
                    ), userId)
            );

            assertNotNull(exception);
            assertEquals("You already have an review on this product", exception.getMessage());
        }
    }

    @Nested
    @DisplayName("тесты метода getReviewsByProduct")
    class GetReviewsByProductTest {

        @Test
        @DisplayName("getReviewsByProduct: если товар есть, отдать отзывы в виде Page")
        public void getReviewsByProduct_requestIsValid_returnPageOfReviewDto() {
            UUID userId1 = UUID.randomUUID();
            UUID userId2 = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            UUID reviewId1 = UUID.randomUUID();
            UUID reviewId2 = UUID.randomUUID();

            PageImpl<Review> reviewPage = new PageImpl<>(List.of(
                    new Review(reviewId1, productId, userId1, 3, "comment", Instant.now(), Instant.now()),
                    new Review(reviewId2, productId, userId2, 3, "comment", Instant.now(), Instant.now())
            ));

            when(reviewRepository.findAllByProductId(productId, Pageable.unpaged()))
                    .thenReturn(reviewPage);

            var reviewDtoPage = defaultReviewService.getReviewsByProduct(productId, Pageable.unpaged());

            verify(reviewRepository, times(1)).findAllByProductId(productId, Pageable.unpaged());
            assertEquals(2, reviewDtoPage.getTotalElements());
        }

        @Test
        @DisplayName("getReviewsByProduct: если отзывов на товар нет, то вернуть пустой объект Page")
        public void getReviewsByProduct_requestIsInvalid_returnEmptyPage() {
            UUID productId = UUID.randomUUID();
            when(reviewRepository.findAllByProductId(productId, Pageable.unpaged()))
                    .thenReturn(new PageImpl<>(List.of()));

            var result = defaultReviewService.getReviewsByProduct(productId, Pageable.unpaged());

            verify(reviewRepository, times(1)).findAllByProductId(productId, Pageable.unpaged());
            assertTrue(result.isEmpty());
            verifyNoMoreInteractions(reviewRepository);
        }
    }

    @Nested
    @DisplayName("Тесты метода updateReview")
    class UpdateReviewTest {

        @Test
        @DisplayName("updateReview: автор отзыва успешно обновляет оценку и комментарий")
        void updateReview_ByAuthor_ShouldUpdateFieldsAndReturnDto() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Review existingReview = Review.builder()
                    .id(reviewId)
                    .productId(productId)
                    .userId(userId)
                    .rating(3)
                    .comment("Старый комментарий")
                    .build();

            UpdateReviewDto updateDto = new UpdateReviewDto(reviewId, 5, "Отличный товар!");

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

            ReviewDto result = defaultReviewService.updateReview(updateDto, userId, false);

            assertNotNull(result);
            assertEquals(reviewId, result.reviewId());
            assertEquals(productId, result.productId());
            assertEquals(userId, result.userId());
            assertEquals(5, result.rating());
            assertEquals("Отличный товар!", result.comment());

            assertEquals(5, existingReview.getRating());
            assertEquals("Отличный товар!", existingReview.getComment());

            verify(reviewRepository, times(1)).findById(reviewId);
        }

        @Test
        @DisplayName("updateReview: администратор может обновить чужой отзыв")
        void updateReview_ByAdmin_ShouldUpdateReview() {
            UUID reviewId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID adminUserId = UUID.randomUUID();

            Review existingReview = Review.builder()
                    .id(reviewId)
                    .userId(authorId)
                    .rating(1)
                    .comment("Плохой товар")
                    .build();

            UpdateReviewDto updateDto = new UpdateReviewDto(reviewId, 4, "Модерировано админом");

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

            ReviewDto result = defaultReviewService.updateReview(updateDto, adminUserId, true);

            assertNotNull(result);
            assertEquals(4, result.rating());
            assertEquals("Модерировано админом", result.comment());
            assertEquals(authorId, result.userId());

            verify(reviewRepository, times(1)).findById(reviewId);
        }

        @Test
        @DisplayName("updateReview: выбрасывает AccessDeniedException, если пользователь не автор и не админ")
        void updateReview_NotAuthorAndNotAdmin_ShouldThrowAccessDeniedException() {
            UUID reviewId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID strangerUserId = UUID.randomUUID();

            Review existingReview = Review.builder()
                    .id(reviewId)
                    .userId(authorId)
                    .rating(5)
                    .comment("Авторский отзыв")
                    .build();

            UpdateReviewDto updateDto = new UpdateReviewDto(reviewId, 1, "Взлом");

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> defaultReviewService.updateReview(updateDto, strangerUserId, false)
            );

            assertEquals("You are not allowed to update this review", exception.getMessage());

            assertEquals(5, existingReview.getRating());
            assertEquals("Авторский отзыв", existingReview.getComment());
        }

        @Test
        @DisplayName("updateReview: выбрасывает NoSuchElementException, если отзыв не найден")
        void updateReview_ReviewNotFound_ShouldThrowNoSuchElementException() {
            UUID reviewId = UUID.randomUUID();
            UpdateReviewDto updateDto = new UpdateReviewDto(reviewId, 5, "Комментарий");

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            NoSuchElementException exception = assertThrows(
                    NoSuchElementException.class,
                    () -> defaultReviewService.updateReview(updateDto, UUID.randomUUID(), false)
            );

            assertEquals("Review with id %s not found".formatted(reviewId), exception.getMessage());
        }
    }

    @Nested
    @DisplayName("Тесты метода deleteReview")
    class DeleteReviewTests {

        @Test
        @DisplayName("deleteReview: автор отзыва успешно удаляет свой отзыв")
        void deleteReview_ByAuthor_ShouldDeleteAndReturnProductId() {
            UUID reviewId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Review existingReview = Review.builder()
                    .id(reviewId)
                    .userId(userId)
                    .productId(productId)
                    .build();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

            UUID resultProductId = defaultReviewService.deleteReview(reviewId, userId, false);

            assertEquals(productId, resultProductId);
            verify(reviewRepository, times(1)).findById(reviewId);
            verify(reviewRepository, times(1)).delete(existingReview);
        }

        @Test
        @DisplayName("deleteReview: администратор успешно удаляет чужой отзыв")
        void deleteReview_ByAdmin_ShouldDeleteAndReturnProductId() {
            UUID reviewId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID adminUserId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();

            Review existingReview = Review.builder()
                    .id(reviewId)
                    .userId(authorId)
                    .productId(productId)
                    .build();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

            UUID resultProductId = defaultReviewService.deleteReview(reviewId, adminUserId, true);

            assertEquals(productId, resultProductId);
            verify(reviewRepository, times(1)).findById(reviewId);
            verify(reviewRepository, times(1)).delete(existingReview);
        }

        @Test
        @DisplayName("deleteReview: выбрасывает AccessDeniedException, если удаляет не автор и не админ")
        void deleteReview_NotAuthorAndNotAdmin_ShouldThrowAccessDeniedException() {
            UUID reviewId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID strangerUserId = UUID.randomUUID();

            Review existingReview = Review.builder()
                    .id(reviewId)
                    .userId(authorId)
                    .productId(UUID.randomUUID())
                    .build();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(existingReview));

            AccessDeniedException exception = assertThrows(
                    AccessDeniedException.class,
                    () -> defaultReviewService.deleteReview(reviewId, strangerUserId, false)
            );

            assertEquals("You are not allowed to delete this review", exception.getMessage());
            verify(reviewRepository, times(1)).findById(reviewId);
            verify(reviewRepository, never()).delete(any());
        }

        @Test
        @DisplayName("deleteReview: выбрасывает NoSuchElementException, если отзыв не найден")
        void deleteReview_ReviewNotFound_ShouldThrowNoSuchElementException() {
            UUID reviewId = UUID.randomUUID();

            when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

            NoSuchElementException exception = assertThrows(
                    NoSuchElementException.class,
                    () -> defaultReviewService.deleteReview(reviewId, UUID.randomUUID(), false)
            );

            assertEquals("Review with id %s not found".formatted(reviewId), exception.getMessage());
            verify(reviewRepository, never()).delete(any());
        }
    }

    @Nested
    @DisplayName("Тесты метода getProductStats")
    class GetProductStatsTests {

        @Test
        @DisplayName("getProductStats: возвращает реальную статистику товара, если она есть в БД")
        void getProductStats_WhenStatsExist_ShouldReturnStats() {
            UUID productId = UUID.randomUUID();
            ReviewStatsDto expectedStats = new ReviewStatsDto(productId, 4.8, 15L);

            when(reviewRepository.getProductStats(productId)).thenReturn(Optional.of(expectedStats));

            ReviewStatsDto actualStats = defaultReviewService.getProductStats(productId);

            assertNotNull(actualStats);
            assertEquals(productId, actualStats.productId());
            assertEquals(4.8, actualStats.averageRating());
            assertEquals(15L, actualStats.totalReviews());

            verify(reviewRepository, times(1)).getProductStats(productId);
        }

        @Test
        @DisplayName("getProductStats: возвращает дефолтную статистику (0.0, 0L), если отзывов нет")
        void getProductStats_WhenStatsNotExist_ShouldReturnDefaultStats() {
            UUID productId = UUID.randomUUID();

            when(reviewRepository.getProductStats(productId)).thenReturn(Optional.empty());

            ReviewStatsDto actualStats = defaultReviewService.getProductStats(productId);

            assertNotNull(actualStats);
            assertEquals(productId, actualStats.productId());
            assertEquals(0.0, actualStats.averageRating());
            assertEquals(0L, actualStats.totalReviews());

            verify(reviewRepository, times(1)).getProductStats(productId);
        }
    }

    @Nested
    @DisplayName("Тесты метода getProductsStats")
    class GetProductsStatsTests {

        @Test
        @DisplayName("getProductsStats: возвращает пустой список, если передан null или пустой список")
        void getProductsStats_NullOrEmptyList_ShouldReturnEmptyListWithoutRepositoryCall() {

            List<ReviewStatsDto> resultNull = defaultReviewService.getProductsStats(null);
            List<ReviewStatsDto> resultEmpty = defaultReviewService.getProductsStats(List.of());

            assertTrue(resultNull.isEmpty());
            assertTrue(resultEmpty.isEmpty());
            verifyNoInteractions(reviewRepository);
        }

        @Test
        @DisplayName("getProductsStats: возвращает корректный список со статистикой и подставляет дефолты для отсутствующих товаров")
        void getProductsStats_ValidProductIds_ShouldMapStatsAndDefaultMissing() {
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();
            UUID productId3 = UUID.randomUUID();

            List<UUID> productIds = List.of(productId1, productId2, productId3);

            ReviewStatsDto stats1 = new ReviewStatsDto(productId1, 4.5, 10L);
            ReviewStatsDto stats3 = new ReviewStatsDto(productId3, 3.0, 2L);

            when(reviewRepository.getProductsStats(productIds)).thenReturn(List.of(stats1, stats3));

            List<ReviewStatsDto> result = defaultReviewService.getProductsStats(productIds);

            assertNotNull(result);
            assertEquals(3, result.size());

            assertEquals(productId1, result.get(0).productId());
            assertEquals(4.5, result.get(0).averageRating());
            assertEquals(10L, result.get(0).totalReviews());

            assertEquals(productId2, result.get(1).productId());
            assertEquals(0.0, result.get(1).averageRating());
            assertEquals(0L, result.get(1).totalReviews());

            assertEquals(productId3, result.get(2).productId());
            assertEquals(3.0, result.get(2).averageRating());
            assertEquals(2L, result.get(2).totalReviews());

            verify(reviewRepository, times(1)).getProductsStats(productIds);
        }
    }
}