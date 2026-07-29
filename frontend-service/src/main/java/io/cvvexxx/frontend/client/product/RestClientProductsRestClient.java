package io.cvvexxx.frontend.client.product;

import io.cvvexxx.frontend.controller.product.payload.NewProductPayload;
import io.cvvexxx.frontend.controller.product.payload.UpdateProductPayload;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.utils.MultipartBodyBuilderUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;


@RequiredArgsConstructor
@Slf4j
public class RestClientProductsRestClient implements ProductsRestClient {

    private static final ParameterizedTypeReference<List<Product>> PRODUCT_LIST_TYPE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final MultipartBodyBuilderUtils builderUtils = new MultipartBodyBuilderUtils();

    @Override
    public List<Product> findAllProducts(String filter) {
        return restClient
                .get()
                .uri("/api/products?filter={filter}", filter)
                .retrieve()
                .body(PRODUCT_LIST_TYPE);
    }

    @Override
    public Optional<Product> findProductById(int productId) {
        try {
            return Optional.ofNullable(restClient
                    .get()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .body(Product.class)
            );
        } catch (HttpClientErrorException.NotFound exception) {
            return Optional.empty();
        }

    }

    @Override
    public Product createProduct(
            String title, String description, BigDecimal price,
            MultipartFile image, UUID createdBy
    ) {
        var builder = builderUtils.multipartBodyBuilder(
                new NewProductPayload(title, description, price, createdBy), image
        );
        try {
            return restClient
                    .post()
                    .uri("/api/products")
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .body(Product.class);
        } catch (HttpClientErrorException.BadRequest exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            throw new BadRequestException((List<String>) problemDetail.getProperties().get("errors"));
        }
    }

    @Override
    public void deleteProduct(int productId) {
        try {
            restClient
                    .delete()
                    .uri("/api/products/{productId}", productId)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.NotFound exception) {
            throw new NoSuchElementException(exception);
        }
    }

    @Override
    public void updateProduct(int productId, String title, String description, BigDecimal price, MultipartFile image) {
        var builder = builderUtils.multipartBodyBuilder(
                new UpdateProductPayload(title, description, price), image
        );

        try {
            restClient
                    .patch()
                    .uri("/api/products/{productId}", productId)
                    .contentType(MediaType.MULTIPART_FORM_DATA)
                    .body(builder.build())
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.BadRequest exception) {
            ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
            throw new BadRequestException((List<String>) problemDetail.getProperties().get("errors"));
        }
    }
}
