package io.cvvexxx.frontend.dto.order;

import java.util.List;

public record NewOrderDto(
        List<NewOrderItemDto> items,
        String deliveryAddress,
        String comment
) {
}
