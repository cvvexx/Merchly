package io.cvvexxx.frontend.service.order;

import io.cvvexxx.frontend.dto.order.OrderDto;
import io.cvvexxx.frontend.view.OrderDetailsView;

import java.util.List;

public interface OrderService {

    List<OrderDetailsView> enrichOrders(List<OrderDto> orders);

    OrderDetailsView enrichOrder(OrderDto order);



}
