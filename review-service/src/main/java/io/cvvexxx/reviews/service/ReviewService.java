package io.cvvexxx.reviews.service;

import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.dto.ReviewStatsDto;
import io.cvvexxx.reviews.dto.UpdateReviewDto;
import io.cvvexxx.reviews.entity.Review;
import io.cvvexxx.reviews.repository.ReviewRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewDto createReview(NewReviewDto newReviewDto, UUID userId) {
        if (reviewRepository.existsByProductIdAndUserId(newReviewDto.productId(), userId)) {
            throw new IllegalStateException("Вы уже оставляли отзыв на этот товар");
        }

        Review review = reviewRepository.save(toEntity(newReviewDto, userId));
        return toDto(review);
    }

    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByProduct(UUID productId, Pageable pageable) {
        Page<Review> reviewsPage = reviewRepository.findAllByProductId(productId, pageable);

        return reviewsPage.map(this::toDto);
    }

    @Transactional
    public ReviewDto updateReview(UpdateReviewDto updateReviewDto, UUID userId, boolean isAdmin) {
        var review = reviewRepository.findById(updateReviewDto.reviewId())
                        .orElseThrow(() -> new NoSuchElementException("Review with id %s not found"
                                .formatted(updateReviewDto.reviewId())));

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to update this review");
        }

        review.setRating(updateReviewDto.rating());
        review.setComment(updateReviewDto.comment());
        return toDto(review);
    }

    @Transactional
    public void deleteReview(UUID reviewId, UUID userId, boolean isAdmin) {
        var review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review with id %s not found"
                        .formatted(reviewId)));

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to delete this review");
        }

        reviewRepository.delete(review);
    }

    @Transactional(readOnly = true)
    public ReviewStatsDto getProductStats(UUID productId) {
        return reviewRepository.getProductStats(productId)
                .orElse(new ReviewStatsDto(productId, 0.0, 0L));
    }

    private ReviewDto toDto(Review review) {
        return new ReviewDto(
                review.getId(),
                review.getProductId(),
                review.getUserId(),
                review.getRating(),
                review.getComment()
        );
    }

    private Review toEntity(NewReviewDto reviewDto, UUID userId) {
        return Review.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .productId(reviewDto.productId())
                .rating(reviewDto.rating())
                .comment(reviewDto.comment())
                .build();
    }

}
