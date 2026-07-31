package io.cvvexxx.reviews.service;

import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.entity.Review;
import io.cvvexxx.reviews.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewRepository reviewRepository;

    public ReviewDto createReview(NewReviewDto newReviewDto, UUID userId) {
        Review savedReview = reviewRepository.save(
                Review.builder()
                        .id(UUID.randomUUID())
                        .userId(userId)
                        .productId(newReviewDto.productId())
                        .rating(newReviewDto.rating())
                        .comment(newReviewDto.comment())
                        .build()
        );

        return new ReviewDto(
                savedReview.getId(),
                savedReview.getProductId(),
                savedReview.getUserId(),
                savedReview.getRating(),
                savedReview.getComment()
        );
    }
}
