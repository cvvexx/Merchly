package io.cvvexxx.frontend.controller.review;

import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.dto.review.NewReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.UpdateReviewDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
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
            RedirectAttributes redirectAttributes
    ) {
        try {
            reviewsRestClient.createReview(newReviewDto);
        } catch (BadRequestException exception) {
            log.warn("Ошибка валидации при создании отзыва: {}", exception.getMessage());

            redirectAttributes.addFlashAttribute("errors", exception.getErrors());
            redirectAttributes.addFlashAttribute("reviewPayload", newReviewDto);
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при создании отзыва", e);
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось добавить отзыв. Попробуйте позже.");
        }

        return "redirect:/catalogue/products/%s".formatted(productId);
    }

    @PatchMapping("{productId}/edit")
    public String updateReview(
            @PathVariable("productId") UUID productId,
            UpdateReviewDto updateReviewDto,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reviewsRestClient.updateReview(updateReviewDto);
        } catch (BadRequestException exception) {
            log.warn("Ошибка при обновлении отзыва: {}", exception.getErrors());
            redirectAttributes.addFlashAttribute("errors", exception.getErrors());
        } catch (Exception e) {
            log.error("Непредвиденная ошибка при обновлении отзыва", e);
            redirectAttributes.addFlashAttribute("errors", List.of("Не удалось обновить отзыв."));
        }

        return "redirect:/catalogue/products/%s".formatted(productId);
    }

    @DeleteMapping("{productId}/{reviewId}")
    public String deleteReview(
            @PathVariable("reviewId") UUID reviewId,
            @PathVariable("productId") UUID productId,
            RedirectAttributes redirectAttributes
    ) {
        try {
            reviewsRestClient.deleteReview(reviewId);
        } catch (Exception e) {
            log.error("Ошибка при удалении отзыва {}", reviewId, e);
            redirectAttributes.addFlashAttribute("errorMessage", "Не удалось удалить отзыв.");
        }

        return "redirect:/catalogue/products/%s".formatted(productId);
    }
}