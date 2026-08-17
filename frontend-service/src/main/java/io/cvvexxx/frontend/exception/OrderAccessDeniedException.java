package io.cvvexxx.frontend.exception;

import java.util.ArrayList;
import java.util.List;

public class OrderAccessDeniedException extends RuntimeException {
    private final List<String> errors;

    public OrderAccessDeniedException(List<String> errors) {
        this.errors = errors;
    }

    public OrderAccessDeniedException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}
