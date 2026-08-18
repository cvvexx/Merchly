package io.cvvexxx.frontend.controller.order;

import io.cvvexxx.frontend.exception.BaseClientException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice(basePackageClasses = OrderController.class)
public class OrderControllerExceptionHandler {

    @ExceptionHandler(BaseClientException.class)
    public String handleBaseClientException(BaseClientException ex,
                                            HttpServletRequest request,
                                            RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("errors", ex.getErrors());

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/orders");
    }
}