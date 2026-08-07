package io.cvvexxx.frontend.client.review;

import io.cvvexxx.frontend.client.review.model.RestPageImpl;
import io.cvvexxx.frontend.dto.review.NewReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewDto;
import io.cvvexxx.frontend.dto.review.ReviewStatsDto;
import io.cvvexxx.frontend.dto.review.UpdateReviewDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import lombok.AllArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

@AllArgsConstructor
public class RestClientReviewsRestClient implements ReviewsRestClient {

    private static final String DEFAULT_API_URI = "/api/reviews/products";
    private final RestClient restClient;

    @Override
    public ReviewDto createReview(NewReviewDto newReviewDto) {
        try {
            return restClient
                    .post()
                    .uri(DEFAULT_API_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(newReviewDto)
                    .retrieve()
                    .body(ReviewDto.class);
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new BadRequestException(extractErrors(exception));
        }
    }

    @Override
    public Page<ReviewDto> getReviews(UUID productId, Pageable pageable) {
        return restClient
                .get()
                .uri(uriBuilder -> {
                            var builder = uriBuilder
                                    .path("/api/reviews/products/{productId}")
                                    .queryParam("page", pageable.getPageNumber())
                                    .queryParam("size", pageable.getPageSize());

                            if (pageable.getSort().isSorted()) {
                                pageable.getSort().forEach(order ->
                                        builder.queryParam(
                                                "sort", order.getProperty() + ","
                                                        + order.getDirection().name().toLowerCase())
                                );
                            }
                            return builder.build(productId);
                        }
                )
                .retrieve()
                .body(new ParameterizedTypeReference<RestPageImpl<ReviewDto>>() {
                });
    }

    @Override
    public ReviewDto updateReview(UpdateReviewDto updateReviewDto) {
        try {
            return restClient
                    .patch()
                    .uri(DEFAULT_API_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(updateReviewDto)
                    .retrieve()
                    .body(ReviewDto.class);
        } catch (HttpClientErrorException.BadRequest exception) {
            throw new BadRequestException(extractErrors(exception));
        }
    }

    @Override
    public void deleteReview(UUID id) {
        restClient
                .delete()
                .uri("%s/{reviewId}".formatted(DEFAULT_API_URI), id)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public ReviewStatsDto getProductReviewStats(UUID id) {
        return restClient
                .get()
                .uri("%s/{productId}/stats".formatted(DEFAULT_API_URI), id)
                .retrieve()
                .body(ReviewStatsDto.class);
    }

    @Override
    public List<ReviewStatsDto> getProductsReviewStats(List<UUID> productIds) {
        return restClient
                .post()
                .uri("%s/stats".formatted(DEFAULT_API_URI))
                .contentType(MediaType.APPLICATION_JSON)
                .body(productIds)
                .retrieve()
                .body(new ParameterizedTypeReference<List<ReviewStatsDto>>() {
                });
    }

    private List<String> extractErrors(HttpClientErrorException.BadRequest exception) {
        ProblemDetail problemDetail = exception.getResponseBodyAs(ProblemDetail.class);
        if (problemDetail == null) {
            return List.of("Неизвестная ошибка сервера");
        }

        if (problemDetail.getProperties() != null && problemDetail.getProperties().containsKey("errors")) {
            Object errorsObj = problemDetail.getProperties().get("errors");
            if (errorsObj instanceof List<?> list) {
                return list.stream()
                        .map(Object::toString)
                        .toList();
            }
        }

        if (problemDetail.getDetail() != null) {
            return List.of(problemDetail.getDetail());
        }

        return List.of("Произошла ошибка при обработке запроса");
    }

}
