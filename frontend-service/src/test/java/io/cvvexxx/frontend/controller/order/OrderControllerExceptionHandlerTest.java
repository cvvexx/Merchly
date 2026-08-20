package io.cvvexxx.frontend.controller.order;

import io.cvvexxx.frontend.exception.BaseClientException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderControllerExceptionHandlerTest {

    private final OrderControllerExceptionHandler handler = new OrderControllerExceptionHandler();

    @Test
    @DisplayName("если в запросе есть заголовок Referer, делает редирект на него и передаёт ошибки как flash-атрибут")
    void handleBaseClientException_WhenRefererPresent_ShouldRedirectToRefererWithFlashErrors() {
        // given
        BaseClientException exception = new BaseClientException(List.of("error1", "error2"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Referer", "/orders/create");
        RedirectAttributes redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        // when
        String result = handler.handleBaseClientException(exception, request, redirectAttributes);

        // then
        assertEquals("redirect:/orders/create", result);
        assertEquals(List.of("error1", "error2"), redirectAttributes.getFlashAttributes().get("errors"));
    }

    @Test
    @DisplayName("если заголовка Referer нет, делает редирект на /orders")
    void handleBaseClientException_WhenRefererMissing_ShouldRedirectToOrders() {
        // given
        BaseClientException exception = new BaseClientException(List.of("error1"));
        MockHttpServletRequest request = new MockHttpServletRequest();
        RedirectAttributes redirectAttributes = new org.springframework.web.servlet.mvc.support.RedirectAttributesModelMap();

        // when
        String result = handler.handleBaseClientException(exception, request, redirectAttributes);

        // then
        assertEquals("redirect:/orders", result);
        assertEquals(List.of("error1"), redirectAttributes.getFlashAttributes().get("errors"));
    }
}
