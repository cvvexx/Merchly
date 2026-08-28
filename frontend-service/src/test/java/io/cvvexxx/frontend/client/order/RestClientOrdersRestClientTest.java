package io.cvvexxx.frontend.client.order;

import io.cvvexxx.frontend.dto.order.NewOrderDto;
import io.cvvexxx.frontend.dto.order.NewOrderItemDto;
import io.cvvexxx.frontend.exception.*;
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
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpStatus.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class RestClientOrdersRestClientTest {

    private MockRestServiceServer server;
    private RestClientOrdersRestClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl("http://localhost");
        server = MockRestServiceServer.bindTo(builder).build();
        client = new RestClientOrdersRestClient(builder.build());
    }

    private NewOrderDto newOrderDto() {
        return new NewOrderDto(List.of(new NewOrderItemDto(UUID.randomUUID(), 1)), "address", "comment");
    }

    @Nested
    @DisplayName("маппинг ошибок createOrder по статус-коду ответа")
    class ErrorMappingTests {

        @Test
        @DisplayName("400 с полем 'errors' в теле -> BadRequestException с этими ошибками")
        void createOrder_When400WithErrorsField_ShouldThrowBadRequestExceptionWithErrors() {
            server.expect(requestTo("http://localhost/api/orders/create"))
                    .andExpect(method(POST))
                    .andRespond(withStatus(BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"deliveryAddress must not be blank\"]}"));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> client.createOrder(newOrderDto()));

            assertEquals(List.of("deliveryAddress must not be blank"), exception.getErrors());
        }

        @Test
        @DisplayName("400 с пустым телом -> BadRequestException с сообщением по умолчанию")
        void createOrder_When400WithEmptyBody_ShouldThrowBadRequestExceptionWithDefaultMessage() {
            server.expect(requestTo("http://localhost/api/orders/create"))
                    .andExpect(method(POST))
                    .andRespond(withStatus(BAD_REQUEST));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> client.createOrder(newOrderDto()));

            assertEquals(List.of("Неизвестная ошибка сервера"), exception.getErrors());
        }

        @Test
        @DisplayName("400 с 'detail', но без 'errors' -> BadRequestException с [detail]")
        void createOrder_When400WithDetailOnly_ShouldThrowBadRequestExceptionWithDetail() {
            server.expect(requestTo("http://localhost/api/orders/create"))
                    .andExpect(method(POST))
                    .andRespond(withStatus(BAD_REQUEST)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"detail\":\"Item is out of stock\"}"));

            BadRequestException exception = assertThrows(BadRequestException.class,
                    () -> client.createOrder(newOrderDto()));

            assertEquals(List.of("Item is out of stock"), exception.getErrors());
        }

        @Test
        @DisplayName("403 -> OrderAccessDeniedException с ошибками из тела ответа")
        void createOrder_When403_ShouldThrowOrderAccessDeniedException() {
            server.expect(requestTo("http://localhost/api/orders/create"))
                    .andExpect(method(POST))
                    .andRespond(withStatus(FORBIDDEN)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"access denied\"]}"));

            OrderAccessDeniedException exception = assertThrows(OrderAccessDeniedException.class,
                    () -> client.createOrder(newOrderDto()));

            assertEquals(List.of("access denied"), exception.getErrors());
        }

        @Test
        @DisplayName("404 -> OrderNotFoundException с ошибками из тела ответа")
        void createOrder_When404_ShouldThrowOrderNotFoundException() {
            server.expect(requestTo("http://localhost/api/orders/create"))
                    .andExpect(method(POST))
                    .andRespond(withStatus(NOT_FOUND)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"order not found\"]}"));

            OrderNotFoundException exception = assertThrows(OrderNotFoundException.class,
                    () -> client.createOrder(newOrderDto()));

            assertEquals(List.of("order not found"), exception.getErrors());
        }

        @Test
        @DisplayName("409 -> OrderCannotChangeStatusException с ошибками из тела ответа")
        void createOrder_When409_ShouldThrowOrderCannotChangeStatusException() {
            server.expect(requestTo("http://localhost/api/orders/create"))
                    .andExpect(method(POST))
                    .andRespond(withStatus(CONFLICT)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"cannot change status\"]}"));

            OrderCannotChangeStatusException exception = assertThrows(OrderCannotChangeStatusException.class,
                    () -> client.createOrder(newOrderDto()));

            assertEquals(List.of("cannot change status"), exception.getErrors());
        }

        @Test
        @DisplayName("неотображённый статус (например 422) -> BaseClientException с ошибками из тела ответа")
        void createOrder_WhenUnmappedStatus_ShouldThrowBaseClientException() {
            server.expect(requestTo("http://localhost/api/orders/create"))
                    .andExpect(method(POST))
                    .andRespond(withStatus(UNPROCESSABLE_ENTITY)
                            .contentType(MediaType.APPLICATION_JSON)
                            .body("{\"errors\":[\"unprocessable\"]}"));

            BaseClientException exception = assertThrows(BaseClientException.class,
                    () -> client.createOrder(newOrderDto()));

            assertEquals(List.of("unprocessable"), exception.getErrors());
        }
    }
}
