package io.cvvexxx.frontend.controller.product;

import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.client.review.ReviewsRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductOwnerDto;
import io.cvvexxx.frontend.dto.review.ReviewStatsDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(printOnlyOnFailure = false)
class ProductsControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Mock
    private ProductsPublicRestClient productsPublicRestClient;

    @Mock
    private UserInternalRestClient userInternalRestClient;

    @Mock
    private ReviewsRestClient reviewsRestClient;

    @Mock
    private ImageUrlFormatter imageUrlFormatter;

    private KeycloakJwtAuthenticationToken mockToken;
    private final UUID userId = UUID.randomUUID();

    // Конфигурация, учит Spring резолвить KeycloakJwtAuthenticationToken в аргументах контроллера
    @TestConfiguration
    static class TestConfig implements WebMvcConfigurer {
        @Override
        public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
            resolvers.add(new HandlerMethodArgumentResolver() {
                @Override
                public boolean supportsParameter(MethodParameter parameter) {
                    return KeycloakJwtAuthenticationToken.class.isAssignableFrom(parameter.getParameterType());
                }

                @Override
                public Object resolveArgument(MethodParameter parameter,
                                              ModelAndViewContainer mavContainer,
                                              NativeWebRequest webRequest,
                                              WebDataBinderFactory binderFactory) {
                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication instanceof KeycloakJwtAuthenticationToken token) {
                        return token;
                    }
                    return null;
                }
            });
        }
    }

    @BeforeEach
    void setUp() {
        mockToken = mock(KeycloakJwtAuthenticationToken.class);
        when(mockToken.isAuthenticated()).thenReturn(true); // Критично для проверки Spring Security
        when(mockToken.getUserId()).thenReturn(userId);
        when(mockToken.getName()).thenReturn("testuser");
    }

    @Test
    void getProductsList_ShouldReturnListViewWithData() throws Exception {
        UUID productId = UUID.randomUUID();
        Product product = new Product(productId, "Title", "Description", BigDecimal.TEN, "image.png", userId);
        ProductOwnerDto owner = new ProductOwnerDto(userId, "testuser", "avatar.png");
        ReviewStatsDto stats = new ReviewStatsDto(productId, 4.5, 10L);

        when(productsPublicRestClient.findAllProducts(any())).thenReturn(List.of(product));
        when(userInternalRestClient.findAllUsersByIds(List.of(userId))).thenReturn(List.of(owner));
        when(reviewsRestClient.getProductsReviewStats(List.of(productId))).thenReturn(List.of(stats));
        when(imageUrlFormatter.getProductImageUrl(any())).thenReturn("/formatted-image.png");

        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/catalogue/products/create")
                .with(authentication(mockToken));

        mockMvc.perform(requestBuilder)
                //then
                .andExpectAll(
                        status().isOk(),
                        view().name("/catalogue/products/list"),
                        model().attributeExists("products")
                );
    }

    @Test
    void getNewProductPage_ShouldReturnNewProductView() throws Exception {
        mockMvc.perform(get("/catalogue/products/create")
                        .with(authentication(mockToken)))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogue/products/new_product"));
    }

    @Test
    void createProduct_Success_ShouldRedirectToProductPage() throws Exception {
        UUID newProductId = UUID.randomUUID();
        Product createdProduct = new Product(newProductId, "New Title", "New Desc", BigDecimal.TEN, null, userId);

        when(productsPublicRestClient.createProduct(eq("New Title"), eq("New Desc"), eq(BigDecimal.TEN), any(), eq(userId)))
                .thenReturn(createdProduct);

        MockMultipartFile imageFile = new MockMultipartFile("image", "test.png", MediaType.IMAGE_PNG_VALUE, "data".getBytes());

        mockMvc.perform(multipart("/catalogue/products/create")
                        .file(imageFile)
                        .param("title", "New Title")
                        .param("description", "New Desc")
                        .param("price", "10")
                        .with(csrf())
                        .with(authentication(mockToken)))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/catalogue/products/" + newProductId));
    }

    @Test
    void createProduct_BadRequest_ShouldReturnFormWithErrors() throws Exception {
        when(productsPublicRestClient.createProduct(any(), any(), any(), any(), any()))
                .thenThrow(new BadRequestException(List.of("Error")));

        MockMultipartFile imageFile = new MockMultipartFile("image", "", "application/octet-stream", new byte[0]);

        mockMvc.perform(multipart("/catalogue/products/create")
                        .file(imageFile)
                        .param("title", "S")
                        .with(csrf())
                        .with(authentication(mockToken)))
                .andExpect(status().isOk())
                .andExpect(view().name("catalogue/products/new_product"))
                .andExpect(model().attributeExists("errors", "payload"));
    }
}