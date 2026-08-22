package io.cvvexxx.users.exceptionHandler;

import io.cvvexxx.users.exception.FieldAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private GlobalExceptionHandler handler;

    @Test
    @DisplayName("handleBindException: возвращает 400 с локализованным заголовком и списком ошибок")
    void handleBindException_ShouldReturnBadRequestWithLocalizedErrors() {
        // given
        var target = new Object();
        var bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.reject("field", "must not be blank");
        BindException exception = new BindException(bindingResult);
        when(messageSource.getMessage(eq("errors.400.title"), any(), eq("errors.400.title"), eq(Locale.ENGLISH)))
                .thenReturn("Bad Request");

        // when
        ResponseEntity<ProblemDetail> response = handler.handleBindException(exception, Locale.ENGLISH);

        // then
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getDetail());
        assertEquals(List.of("must not be blank"), response.getBody().getProperties().get("errors"));
    }

    @Test
    @DisplayName("handleBadCredentialsException: возвращает 401 с сообщением об ошибке аутентификации")
    void handleBadCredentialsException_ShouldReturnUnauthorized() {
        // given
        BadCredentialsException exception = new BadCredentialsException("invalid password");

        // when
        ResponseEntity<?> response = handler.handleBadCredentialsException(exception);

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("invalid password"));
    }

    @Test
    @DisplayName("handleAuthenticationException: возвращает 401 с сообщением об отказе в доступе")
    void handleAuthenticationException_ShouldReturnUnauthorized() {
        // given
        AuthenticationException exception = new AuthenticationException("token expired") {
        };

        // when
        ResponseEntity<?> response = handler.handleAuthenticationException(exception);

        // then
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertTrue(response.getBody().toString().contains("token expired"));
    }

    @Test
    @DisplayName("handleFieldAlreadyExists: возвращает 409 с деталями конфликтующего поля")
    void handleFieldAlreadyExists_ShouldReturnConflictWithFieldDetails() {
        // given
        FieldAlreadyExistsException exception = new FieldAlreadyExistsException("username", "username already exists");

        // when
        ResponseEntity<ProblemDetail> response = handler.handleFieldAlreadyExists(exception);

        // then
        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("username already exists", response.getBody().getDetail());
        assertEquals("DUPLICATE_FIELD", response.getBody().getProperties().get("code"));
        assertEquals("username", response.getBody().getProperties().get("field"));
    }

    @Test
    @DisplayName("handleGenericException: возвращает 500 с общим сообщением об ошибке")
    void handleGenericException_ShouldReturnInternalServerError() {
        // given
        Exception exception = new RuntimeException("unexpected failure");

        // when
        ResponseEntity<?> response = handler.handleGenericException(exception);

        // then
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals("An unexpected internal server error occurred.", response.getBody());
    }
}
