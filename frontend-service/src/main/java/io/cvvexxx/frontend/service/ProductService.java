package io.cvvexxx.frontend.service;

import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductListData;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;
import io.cvvexxx.frontend.dto.product.ProductPageData;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewStatsDto;
import io.cvvexxx.frontend.dto.review.UserReviewDto;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import io.cvvexxx.frontend.view.ProductDetailsViewModel;
import io.cvvexxx.frontend.view.ProductOwnerViewModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ProductService {

    private final ProductsPublicRestClient productsPublicRestClient;
    private final UserInternalRestClient userInternalRestClient;
    private final ReviewsRestClient reviewsRestClient;
    private final ImageUrlFormatter imageUrlFormatter;

    public ProductListData getProductsList(String filter) {
        List<Product> products = this.productsPublicRestClient.findAllProducts(filter);

        if (products.isEmpty()) {
            return new ProductListData(List.of());
        }

        List<UUID> creatorIds = products.stream()
                .map(Product::createdBy)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        List<ProductOwnerDto> creators = this.userInternalRestClient.findAllUsersByIds(creatorIds);
        Map<UUID, ProductOwnerDto> creatorsMap = creators.stream()
                .collect(Collectors.toMap(ProductOwnerDto::id, Function.identity()));

        List<UUID> productIds = products.stream()
                .map(Product::id)
                .toList();

        List<ReviewStatsDto> reviewStatsList = this.reviewsRestClient.getProductsReviewStats(productIds);

        Map<UUID, ReviewStatsDto> statsMap = (reviewStatsList != null ? reviewStatsList : List.<ReviewStatsDto>of())
                .stream()
                .collect(Collectors.toMap(ReviewStatsDto::productId, Function.identity()));

        List<ProductOwnerViewModel> viewModels = products.stream()
                .map(product -> {
                    ProductOwnerDto creator = creatorsMap.get(product.createdBy());
                    ReviewStatsDto stats = statsMap.getOrDefault(
                            product.id(),
                            new ReviewStatsDto(product.id(), 0.0, 0L)
                    );
                    return new ProductOwnerViewModel(
                            product,
                            creator,
                            imageUrlFormatter.getProductImageUrl(product.imageFileName()),
                            imageUrlFormatter.getUserAvatarUrl(creator.avatarFileName()),
                            stats.totalReviews(),
                            stats.averageRating()
                    );
                })
                .toList();

        return new ProductListData(viewModels);
    }

    public ProductPageData getProductPage(Product product, Pageable pageable, KeycloakJwtAuthenticationToken token) {
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
                imageUrlFormatter.getProductImageUrl(product.imageFileName()),
                imageUrlFormatter.getUserAvatarUrl(user.avatarFileName()),
                reviewsPage,
                totalReviews,
                avgRating
        );

        boolean isAdmin = token.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        String authUsername = token.getName();

        return new ProductPageData(viewModel, isAdmin, authUsername);
    }
}
