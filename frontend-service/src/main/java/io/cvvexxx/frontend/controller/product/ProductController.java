package io.cvvexxx.frontend.controller.product;

import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.controller.product.payload.UpdateProductPayload;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewStatsDto;
import io.cvvexxx.frontend.dto.review.UserReviewDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import io.cvvexxx.frontend.view.ProductDetailsViewModel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("catalogue/products/{productId}")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductsPublicRestClient productsPublicRestClient;
    private final UserInternalRestClient userInternalRestClient;
    private final UserPublicRestClient userPublicRestClient;
    private final ReviewsRestClient reviewsRestClient;
    private final MessageSource messageSource;
    private final ImageUrlFormatter imageUrlFormatter;

    @ModelAttribute("product")
    public Product product(@PathVariable("productId") UUID productId) {
        return productsPublicRestClient.findProductById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));
    }

    @GetMapping
    public String getProductPage(
            @ModelAttribute("product") Product product,
            @PageableDefault(size = 5) Pageable pageable,
            Model model,
            KeycloakJwtAuthenticationToken token
    ) {
        ProductOwnerDto user = userInternalRestClient.findUserById(product.createdBy());

        Page<ReviewDto> reviews = reviewsRestClient.getReviews(product.id(), pageable);

        List<UUID> reviewerIds = reviews.getContent().stream()
                .map(ReviewDto::userId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        Map<UUID, String> usernamesMap;

        if (!reviewerIds.isEmpty()) {
            usernamesMap = userInternalRestClient.findAllUsersByIds(reviewerIds).stream()
                    .collect(Collectors.toMap(
                            ProductOwnerDto::id,
                            ProductOwnerDto::username
                    ));
        } else {
            usernamesMap = Map.of();
        }

        Page<UserReviewDto> reviewsPage = reviews.map(reviewDto -> new UserReviewDto(
                reviewDto.reviewId(),
                reviewDto.productId(),
                reviewDto.userId(),
                usernamesMap.get(reviewDto.userId()),
                reviewDto.rating(),
                reviewDto.comment()
        ));
        log.info("reviewsPage {}", reviewsPage);

        ReviewStatsDto stats = reviewsRestClient.getProductReviewStats(product.id());

        long totalReviews = (stats != null) ? stats.totalReviews() : 0L;
        double avgRating = (stats != null) ? stats.averageRating() : 0.0;

        ProductDetailsViewModel viewModel = new ProductDetailsViewModel(
                product,
                user,
                getImageUrl(product),
                getUserAvatarUrl(user),
                reviewsPage,
                totalReviews,
                avgRating
        );

        boolean isAdmin = token.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String authUsername = token.getName();

        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("authUsername", authUsername);
        model.addAttribute("data", viewModel);

        return "catalogue/products/product";
    }

    @GetMapping("edit")
    public String getProductEditPage() {
        return "catalogue/products/edit";
    }

    @PostMapping("edit")
    public String updateProduct(
            @ModelAttribute("product") Product product,
            MultipartFile image,
            UpdateProductPayload payload,
            Model model
    ) {
        try {
            productsPublicRestClient.updateProduct(
                    product.id(),
                    payload.title(),
                    payload.description(),
                    payload.price(),
                    image
            );
            return "redirect:/catalogue/products/%s".formatted(product.id());
        } catch (BadRequestException exception) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            return "catalogue/products/edit";
        }
    }

    @PostMapping("delete")
    public String deleteProduct(@ModelAttribute("product") Product product) {
        productsPublicRestClient.deleteProduct(product.id());
        return "redirect:/catalogue/products/list";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElementException(
            NoSuchElementException exception,
            Locale locale,
            HttpServletResponse response,
            Model model
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("error",
                messageSource.getMessage(exception.getMessage(), new Object[0],
                        exception.getMessage(), locale));

        return "error/404";
    }

    @RequestMapping("error-403")
    public String accessDenied() {
        return "error/403";
    }

    private String getImageUrl(Product product) {
        return (product != null && product.imageFileName() != null && !product.imageFileName().isBlank())
                ? imageUrlFormatter.getProductImageUrl(product.imageFileName())
                : "/images/default-product-image.png";
    }

    private String getUserAvatarUrl(ProductOwnerDto creator) {
        if (creator == null || creator.avatarFileName() == null || creator.avatarFileName().isBlank()) {
            return "/images/default-user-avatar.png";
        }
        return imageUrlFormatter.getUserAvatarUrl(creator.avatarFileName());
    }
}