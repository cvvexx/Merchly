package io.cvvexxx.orders.controller;

import io.cvvexxx.orders.exception.OrderAccessDeniedException;
import io.cvvexxx.orders.exception.OrderCannotCancelException;
import io.cvvexxx.orders.exception.OrderCannotConfirmException;
import io.cvvexxx.orders.exception.OrderNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    private static final Locale LOCALE = Locale.forLanguageTag("ru");

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    @DisplayName("handleOrderNotFoundException: возвращает 404 с локализованным сообщением и args из исключения")
    void handleOrderNotFoundException_ShouldReturn404WithLocalizedMessage() {
        UUID orderId = UUID.randomUUID();
        OrderNotFoundException exception = new OrderNotFoundException(orderId);
        when(messageSource.getMessage(eq("order.errors.order_not_found"), eq(new Object[]{orderId}), anyString(), eq(LOCALE)))
                .thenReturn("Заказ не найден");

        ResponseEntity<ProblemDetail> response = handler.handleOrderNotFoundException(exception, LOCALE);

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals("Заказ не найден", response.getBody().getDetail());
        assertEquals(List.of("Заказ не найден"), response.getBody().getProperties().get("errors"));
    }

    @Test
    @DisplayName("handleOrderAccessDeniedException: возвращает 403 с локализованным сообщением")
    void handleOrderAccessDeniedException_ShouldReturn403WithLocalizedMessage() {
        OrderAccessDeniedException exception = new OrderAccessDeniedException();
        when(messageSource.getMessage(eq("order.errors.access_denied"), eq(new Object[0]), anyString(), eq(LOCALE)))
                .thenReturn("Нет доступа");

        ResponseEntity<ProblemDetail> response = handler.handleOrderAccessDeniedException(exception, LOCALE);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("Нет доступа", response.getBody().getDetail());
    }

    @Test
    @DisplayName("handleOrderCannotCancelException: возвращает 409 с локализованным сообщением")
    void handleOrderCannotCancelException_ShouldReturn409WithLocalizedMessage() {
        UUID orderId = UUID.randomUUID();
        OrderCannotCancelException exception = new OrderCannotCancelException(orderId);
        when(messageSource.getMessage(eq("order.errors.cannot_cancel"), eq(new Object[]{orderId}), anyString(), eq(LOCALE)))
                .thenReturn("Заказ нельзя отменить");

        ResponseEntity<ProblemDetail> response = handler.handleOrderCannotCancelException(exception, LOCALE);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Заказ нельзя отменить", response.getBody().getDetail());
    }

    @Test
    @DisplayName("handleOrderCannotConfirmException: возвращает 409 с локализованным сообщением")
    void handleOrderCannotConfirmException_ShouldReturn409WithLocalizedMessage() {
        UUID orderId = UUID.randomUUID();
        OrderCannotConfirmException exception = new OrderCannotConfirmException(orderId);
        when(messageSource.getMessage(eq("order.errors.cannot_confirm"), eq(new Object[]{orderId}), anyString(), eq(LOCALE)))
                .thenReturn("Заказ нельзя подтвердить");

        ResponseEntity<ProblemDetail> response = handler.handleOrderCannotConfirmException(exception, LOCALE);

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Заказ нельзя подтвердить", response.getBody().getDetail());
    }

    @Nested
    @DisplayName("handleBindException")
    class HandleBindExceptionTests {

        @Test
        @DisplayName("собирает локализованные сообщения по всем ошибкам валидации в errors")
        void handleBindException_ShouldReturn400WithLocalizedFieldErrors() {
            Object target = new Object();
            var bindingResult = new BeanPropertyBindingResult(target, "newOrderDto");
            bindingResult.addError(new FieldError("newOrderDto", "deliveryAddress", "must not be blank"));
            BindException exception = new BindException(bindingResult);
            when(messageSource.getMessage(eq("errors.400.title"), any(), anyString(), eq(LOCALE)))
                    .thenReturn("Некорректный запрос");
            when(messageSource.getMessage(any(FieldError.class), eq(LOCALE)))
                    .thenReturn("Адрес доставки обязателен");

            ResponseEntity<ProblemDetail> response = handler.handleBindException(exception, LOCALE);

            assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
            assertEquals("Некорректный запрос", response.getBody().getDetail());
            assertEquals(List.of("Адрес доставки обязателен"), response.getBody().getProperties().get("errors"));
        }
    }
}
