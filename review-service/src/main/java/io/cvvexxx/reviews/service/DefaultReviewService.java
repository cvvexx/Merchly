package io.cvvexxx.reviews.service;

import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.dto.ReviewStatsDto;
import io.cvvexxx.reviews.dto.UpdateReviewDto;
import io.cvvexxx.reviews.entity.Review;
import io.cvvexxx.reviews.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultReviewService implements ReviewService {

    private final ReviewRepository reviewRepository;
    private static final String CACHE_PRODUCT_STATS = "productStats";

    @Override
    @Transactional
    @CacheEvict(value = CACHE_PRODUCT_STATS, key = "#newReviewDto.productId()")
    public ReviewDto createReview(NewReviewDto newReviewDto, UUID userId) {
        if (reviewRepository.existsByProductIdAndUserId(newReviewDto.productId(), userId)) {
            throw new IllegalStateException("You already have an review on this product");
        }

        Review review = reviewRepository.save(toEntity(newReviewDto, userId));
        return toDto(review);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDto> getReviewsByProduct(UUID productId, Pageable pageable) {
        Page<Review> reviewsPage = reviewRepository.findAllByProductId(productId, pageable);

        return reviewsPage.map(this::toDto);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_PRODUCT_STATS, key = "#result.productId()")
    public ReviewDto updateReview(UpdateReviewDto updateReviewDto, UUID userId, boolean isAdmin) {
        var review = reviewRepository.findById(updateReviewDto.reviewId())
                .orElseThrow(() -> new NoSuchElementException("Review with id %s not found".formatted(updateReviewDto.reviewId())));

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to update this review");
        }

        review.setRating(updateReviewDto.rating());
        review.setComment(updateReviewDto.comment());

        return toDto(review);
    }

    @Override
    @Transactional
    @CacheEvict(value = CACHE_PRODUCT_STATS, key = "#result")
    public UUID deleteReview(UUID reviewId, UUID userId, boolean isAdmin) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new NoSuchElementException("Review with id %s not found".formatted(reviewId)));

        if (!isAdmin && !review.getUserId().equals(userId)) {
            throw new AccessDeniedException("You are not allowed to delete this review");
        }

        reviewRepository.delete(review);
        
        return review.getProductId();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_PRODUCT_STATS, key = "#productId")
    public ReviewStatsDto getProductStats(UUID productId) {
        return reviewRepository.getProductStats(productId)
                .orElse(new ReviewStatsDto(productId, 0.0, 0L));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReviewStatsDto> getProductsStats(List<UUID> productIds) {
        if (productIds == null || productIds.isEmpty()) {
            return List.of();
        }

        List<ReviewStatsDto> stats = reviewRepository.getProductsStats(productIds);
        Map<UUID, ReviewStatsDto> statsMap = stats.stream()
                .collect(Collectors.toMap(ReviewStatsDto::productId, Function.identity()));

        return productIds.stream()
                .map(id -> statsMap.getOrDefault(id, new ReviewStatsDto(id, 0.0, 0L)))
                .toList();
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
