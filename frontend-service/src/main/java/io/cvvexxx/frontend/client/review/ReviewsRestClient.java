package io.cvvexxx.frontend.client.review;

import io.cvvexxx.frontend.dto.review.NewReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewStatsDto;
import io.cvvexxx.frontend.dto.review.UpdateReviewDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ReviewsRestClient {

    ReviewDto createReview(NewReviewDto newReviewDto);

    Page<ReviewDto> getReviews(UUID productId, Pageable pageable);

    ReviewDto updateReview(UpdateReviewDto updateReviewDto);

    void deleteReview(UUID id);

    ReviewStatsDto getProductReviewStats(UUID id);

    List<ReviewStatsDto> getProductsReviewStats(List<UUID> productIds);
}