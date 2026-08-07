package io.cvvexxx.reviews.service;

import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.dto.ReviewStatsDto;
import io.cvvexxx.reviews.dto.UpdateReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReviewService {

    ReviewDto createReview(NewReviewDto newReviewDto, UUID userId);

    Page<ReviewDto> getReviewsByProduct(UUID productId, Pageable pageable);

    ReviewDto updateReview(UpdateReviewDto updateReviewDto, UUID userId, boolean isAdmin);

    UUID deleteReview(UUID reviewId, UUID userId, boolean isAdmin);

    ReviewStatsDto getProductStats(UUID productId);

    List<ReviewStatsDto> getProductsStats(List<UUID> productIds);
}
