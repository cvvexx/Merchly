package io.cvvexxx.frontend.client.review;

import io.cvvexxx.frontend.dto.review.NewReviewDto;
import io.cvvexxx.frontend.dto.review.UpdateReviewDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestClientReviewsRestClientTest {

    private MockRestServiceServer server;
    private RestClientReviewsRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientReviewsRestClient(builder.build());
    }

    @Nested
    @DisplayName("createReview")
    class CreateReviewTests {

        @Test
        @DisplayName("при 400 с полем 'errors' выбрасывает BadRequestException с этими ошибками")
        void createReview_When400WithErrorsField_ShouldThrowBadRequestExceptionWithErrors() {
            server.expect(requestTo("http://localhost/api/reviews/products"))
                    .andRespond(withStatus(BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"rating must be <= 5\"]}"));
            NewReviewDto dto = new NewReviewDto(UUID.randomUUID(), 6, "invalid");

            BadRequestException exception = assertThrows(BadRequestException.class, () -> client.createReview(dto));

            assertEquals(List.of("rating must be <= 5"), exception.getErrors());
        }

        @Test
        @DisplayName("при 400 с пустым телом выбрасывает BadRequestException с сообщением по умолчанию")
        void createReview_When400WithEmptyBody_ShouldThrowBadRequestExceptionWithDefaultMessage() {
            server.expect(requestTo("http://localhost/api/reviews/products"))
                    .andRespond(withStatus(BAD_REQUEST));
            NewReviewDto dto = new NewReviewDto(UUID.randomUUID(), 6, "invalid");

            BadRequestException exception = assertThrows(BadRequestException.class, () -> client.createReview(dto));

            assertEquals(List.of("Неизвестная ошибка сервера"), exception.getErrors());
        }
    }

    @Nested
    @DisplayName("updateReview")
    class UpdateReviewTests {

        @Test
        @DisplayName("при 400 с 'detail', но без 'errors' выбрасывает BadRequestException с [detail]")
        void updateReview_When400WithDetailOnly_ShouldThrowBadRequestExceptionWithDetail() {
            server.expect(requestTo("http://localhost/api/reviews/products"))
                    .andRespond(withStatus(BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"detail\":\"Review not found\"}"));
            UpdateReviewDto dto = new UpdateReviewDto(UUID.randomUUID(), 4, "ok");

            BadRequestException exception = assertThrows(BadRequestException.class, () -> client.updateReview(dto));

            assertEquals(List.of("Review not found"), exception.getErrors());
        }
    }
}
