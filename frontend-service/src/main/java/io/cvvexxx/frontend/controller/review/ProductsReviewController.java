package io.cvvexxx.frontend.controller.review;

import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.dto.review.NewReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.UpdateReviewDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Controller
@RequestMapping("/catalogue/reviews")
@Slf4j
@RequiredArgsConstructor
public class ProductsReviewController {

    private final ReviewsRestClient reviewsRestClient;

    @PostMapping("{productId}/create")
    public String createReview(
            @PathVariable UUID productId,
            NewReviewDto newReviewDto,
            Model model
    ) {
        try {
            reviewsRestClient.createReview(newReviewDto);
        } catch (BadRequestException exception) {
            model.addAttribute("errors", exception.getMessage());
        }
        return "redirect:/catalogue/products/%s".formatted(productId);
    }

    @PatchMapping("/edit")
    public String updateReview(
            UpdateReviewDto updateReviewDto
    ) {
        ReviewDto reviewDto = reviewsRestClient.updateReview(updateReviewDto);

        return "redirect:/catalogue/products/%s".formatted(reviewDto.productId());
    }

    @DeleteMapping("{productId}/{reviewId}")
    public String deleteReview(
            @PathVariable("reviewId") UUID reviewId,
            @PathVariable("productId") UUID productId
    ) {
        reviewsRestClient.deleteReview(reviewId);

        return "redirect:/catalogue/products/%s".formatted(productId);
    }
}
