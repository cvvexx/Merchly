package io.cvvexxx.frontend.exception;

import lombok.Getter;

import java.util.List;

@Getter
public class BaseClientException extends RuntimeException {
    private final List<String> errors;

    public BaseClientException(List<String> errors) {
        this.errors = errors;
    }

    public BaseClientException(String message, List<String> errors) {
        super(message);
        this.errors = errors;
    }
}
