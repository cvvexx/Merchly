package io.cvvexxx.frontend.service.product;

import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductListData;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;
import io.cvvexxx.frontend.dto.product.ProductPageData;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewStatsDto;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import io.cvvexxx.frontend.view.ProductOwnerViewModel;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultProductServiceTest {

    @Mock
    private ProductsPublicRestClient productsPublicRestClient;

    @Mock
    private UserInternalRestClient userInternalRestClient;

    @Mock
    private ReviewsRestClient reviewsRestClient;

    @Mock
    private ImageUrlFormatter imageUrlFormatter;

    @InjectMocks
    private DefaultProductService productService;

    private Product product(UUID productId, UUID creatorId) {
        return new Product(productId, "title", "desc", 1, BigDecimal.TEN, "image.png", creatorId);
    }

    private KeycloakJwtAuthenticationToken tokenWithRoles(String username, List<String> roles) {
        return new KeycloakJwtAuthenticationToken(
                username,
                UUID.randomUUID(),
                "access-token",
                "refresh-token",
                roles.stream()
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList()
        );
    }

    @Nested
    @DisplayName("getProductsList")
    class GetProductsListTests {

        @Test
        @DisplayName("если product-service вернул пустой список, не обращается к user/review сервисам")
        void getProductsList_WhenNoProducts_ShouldReturnEmptyDataWithoutFurtherCalls() {
            // given
            when(productsPublicRestClient.findAllProducts("filter")).thenReturn(List.of());

            // when
            ProductListData result = productService.getProductsList("filter");

            // then
            assertEquals(List.of(), result.viewModels());
            verifyNoInteractions(userInternalRestClient, reviewsRestClient);
        }

        @Test
        @DisplayName("собирает viewModel из товара, владельца и статистики отзывов; если статистики нет - подставляет нулевую заглушку")
        void getProductsList_ShouldEnrichProductsWithOwnersAndReviewStats() {
            // given
            UUID creatorId = UUID.randomUUID();
            UUID productWithStatsId = UUID.randomUUID();
            UUID productWithoutStatsId = UUID.randomUUID();

            Product productWithStats = product(productWithStatsId, creatorId);
            Product productWithoutStats = product(productWithoutStatsId, creatorId);

            when(productsPublicRestClient.findAllProducts(null))
                    .thenReturn(List.of(productWithStats, productWithoutStats));

            ProductOwnerDto owner = new ProductOwnerDto(creatorId, "creator", "avatar.png");
            when(userInternalRestClient.findAllUsersByIds(List.of(creatorId))).thenReturn(List.of(owner));

            when(reviewsRestClient.getProductsReviewStats(List.of(productWithStatsId, productWithoutStatsId)))
                    .thenReturn(List.of(new ReviewStatsDto(productWithStatsId, 4.5, 10L)));

            when(imageUrlFormatter.getProductImageUrl("image.png")).thenReturn("/img/product.png");
            when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");

            // when
            ProductListData result = productService.getProductsList(null);

            // then
            assertEquals(2, result.viewModels().size());
            ProductOwnerViewModel withStats = result.viewModels().stream()
                    .filter(vm -> vm.product().id().equals(productWithStatsId))
                    .findFirst().orElseThrow();
            ProductOwnerViewModel withoutStats = result.viewModels().stream()
                    .filter(vm -> vm.product().id().equals(productWithoutStatsId))
                    .findFirst().orElseThrow();

            assertEquals(10L, withStats.reviewsCount());
            assertEquals(4.5, withStats.averageRating());
            assertEquals(0L, withoutStats.reviewsCount());
            assertEquals(0.0, withoutStats.averageRating());
            assertEquals("/img/product.png", withStats.productImageUrl());
            assertEquals("/img/avatar.png", withStats.userAvatarUrl());
        }

        @Test
        @DisplayName("если reviewsRestClient вернул null, обрабатывает это как пустую статистику")
        void getProductsList_WhenReviewStatsIsNull_ShouldTreatAsEmptyStats() {
            // given
            UUID creatorId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = product(productId, creatorId);

            when(productsPublicRestClient.findAllProducts(null)).thenReturn(List.of(product));
            when(userInternalRestClient.findAllUsersByIds(List.of(creatorId)))
                    .thenReturn(List.of(new ProductOwnerDto(creatorId, "creator", "avatar.png")));
            when(reviewsRestClient.getProductsReviewStats(List.of(productId))).thenReturn(null);
            when(imageUrlFormatter.getProductImageUrl("image.png")).thenReturn("/img/product.png");
            when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");

            // when
            ProductListData result = productService.getProductsList(null);

            // then
            assertEquals(1, result.viewModels().size());
            assertEquals(0L, result.viewModels().get(0).reviewsCount());
            assertEquals(0.0, result.viewModels().get(0).averageRating());
        }
    }

    @Nested
    @DisplayName("getProductPage")
    class GetProductPageTests {

        @Test
        @DisplayName("определяет isAdmin=true, если токен содержит ROLE_ADMIN, и подставляет имена рецензентов")
        void getProductPage_WhenTokenHasAdminRole_ShouldReturnAdminTrueAndReviewerUsernames() {
            // given
            UUID creatorId = UUID.randomUUID();
            UUID reviewerId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = product(productId, creatorId);
            Pageable pageable = PageRequest.of(0, 5);

            ProductOwnerDto creator = new ProductOwnerDto(creatorId, "creator", "avatar.png");
            when(userInternalRestClient.findUserById(creatorId)).thenReturn(creator);

            ReviewDto review = new ReviewDto(UUID.randomUUID(), productId, reviewerId, 5, "great");
            Page<ReviewDto> reviewsPage = new PageImpl<>(List.of(review), pageable, 1);
            when(reviewsRestClient.getReviews(productId, pageable)).thenReturn(reviewsPage);

            when(userInternalRestClient.findAllUsersByIds(List.of(reviewerId)))
                    .thenReturn(List.of(new ProductOwnerDto(reviewerId, "reviewer", "reviewer.png")));

            when(reviewsRestClient.getProductReviewStats(productId))
                    .thenReturn(new ReviewStatsDto(productId, 4.0, 1L));

            when(imageUrlFormatter.getProductImageUrl("image.png")).thenReturn("/img/product.png");
            when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");

            KeycloakJwtAuthenticationToken token = tokenWithRoles("admin-user", List.of("ROLE_ADMIN"));

            // when
            ProductPageData result = productService.getProductPage(product, pageable, token);

            // then
            assertTrue(result.isAdmin());
            assertEquals("admin-user", result.authUsername());
            assertEquals("reviewer", result.viewModel().reviews().getContent().get(0).username());
            assertEquals(1L, result.viewModel().reviewsCount());
            assertEquals(4.0, result.viewModel().averageRating());
        }

        @Test
        @DisplayName("если у отзывов нет userId, не обращается к user-service за именами и isAdmin=false для не-админа")
        void getProductPage_WhenNoReviewers_ShouldSkipUsernameLookupAndReturnAdminFalse() {
            // given
            UUID creatorId = UUID.randomUUID();
            UUID productId = UUID.randomUUID();
            Product product = product(productId, creatorId);
            Pageable pageable = PageRequest.of(0, 5);

            when(userInternalRestClient.findUserById(creatorId))
                    .thenReturn(new ProductOwnerDto(creatorId, "creator", "avatar.png"));
            when(reviewsRestClient.getReviews(productId, pageable))
                    .thenReturn(new PageImpl<>(List.of(), pageable, 0));
            when(reviewsRestClient.getProductReviewStats(productId)).thenReturn(null);
            when(imageUrlFormatter.getProductImageUrl("image.png")).thenReturn("/img/product.png");
            when(imageUrlFormatter.getUserAvatarUrl("avatar.png")).thenReturn("/img/avatar.png");

            KeycloakJwtAuthenticationToken token = tokenWithRoles("plain-user", List.of("ROLE_USER"));

            // when
            ProductPageData result = productService.getProductPage(product, pageable, token);

            // then
            assertTrue(!result.isAdmin());
            assertEquals(0L, result.viewModel().reviewsCount());
            assertEquals(0.0, result.viewModel().averageRating());
        }
    }
}
