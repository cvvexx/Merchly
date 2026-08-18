package io.cvvexxx.frontend.exception;

import java.util.List;

public class OrderNotFoundException extends BaseClientException {
    public OrderNotFoundException(List<String> errors) {
        super(errors);
    }
}
