package io.cvvexxx.reviews.controller;

import io.cvvexxx.reviews.dto.NewReviewDto;
import io.cvvexxx.reviews.dto.ReviewDto;
import io.cvvexxx.reviews.dto.ReviewStatsDto;
import io.cvvexxx.reviews.dto.UpdateReviewDto;
import io.cvvexxx.reviews.service.ReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@RequestMapping("api/reviews/products")
@Slf4j
public class ProductsReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public ResponseEntity<ReviewDto> createReview(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NewReviewDto newReviewDto,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));

            return ResponseEntity.ok(reviewService.createReview(newReviewDto, userId));
        }
    }

    @GetMapping("/{productId}")
    public ResponseEntity<Page<ReviewDto>> getReviews(
            @PathVariable UUID productId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ResponseEntity.ok(reviewService.getReviewsByProduct(productId, pageable));
    }

    @PatchMapping
    public ResponseEntity<ReviewDto> updateReview(
            @Valid @RequestBody UpdateReviewDto updateReviewDto,
            @AuthenticationPrincipal Jwt jwt,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));

            return ResponseEntity.ok(reviewService.updateReview(updateReviewDto, userId, hasAdminRole(jwt)));
        }
    }

    @DeleteMapping("/{reviewId}")
    public ResponseEntity<Void> deleteReview(
            @PathVariable UUID reviewId,
            @AuthenticationPrincipal Jwt jwt
    ) {
        UUID userId = UUID.fromString(jwt.getClaimAsString("sub"));

        reviewService.deleteReview(reviewId, userId, hasAdminRole(jwt));
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{productId}/stats")
    public ResponseEntity<ReviewStatsDto> getProductStats(
            @PathVariable("productId") UUID productId
    ) {
        return ResponseEntity.ok(reviewService.getProductStats(productId));
    }

    @PostMapping("/stats")
    public ResponseEntity<List<ReviewStatsDto>> getProductsStats(
            @RequestBody List<UUID> productIds
    ) {
        return ResponseEntity.ok(reviewService.getProductsStats(productIds));
    }


    private boolean hasAdminRole(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess != null && realmAccess.get("roles") instanceof List<?> roles) {
            return roles.contains("ADMIN") || roles.contains("ROLE_ADMIN");
        }
        return false;
    }
}
