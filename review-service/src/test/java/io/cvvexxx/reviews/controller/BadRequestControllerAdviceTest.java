package io.cvvexxx.reviews.controller;

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
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BadRequestControllerAdviceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private BadRequestControllerAdvice advice;

    @Test
    @DisplayName("handleBindException: возвращает 400 с заголовком из MessageSource и списком сообщений об ошибках")
    void handleBindException_WhenBindingResultHasErrors_ShouldReturnBadRequestWithErrorMessages() {
        var target = new Object();
        var bindingResult = new BeanPropertyBindingResult(target, "target");
        bindingResult.reject("field1", "must not be blank");
        bindingResult.reject("field2", "must be positive");
        BindException exception = new BindException(bindingResult);
        Locale locale = Locale.ENGLISH;
        when(messageSource.getMessage("errors.400.title", new Object[0], "errors.400.title", locale))
                .thenReturn("Bad Request");

        ResponseEntity<ProblemDetail> response = advice.handleBindException(exception, locale);

        assertNotNull(response.getBody());
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Bad Request", response.getBody().getDetail());
        assertEquals(List.of("must not be blank", "must be positive"), response.getBody().getProperties().get("errors"));
    }
}
