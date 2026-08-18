package io.cvvexxx.frontend.exception;

import java.util.List;

public class OrderAccessDeniedException extends BaseClientException {

    public OrderAccessDeniedException(List<String> errors) {
        super(errors);
    }
}
