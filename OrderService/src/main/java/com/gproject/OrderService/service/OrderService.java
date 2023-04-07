package com.gproject.OrderService.service;

import com.gproject.OrderService.model.OrderRequest;
import com.gproject.OrderService.model.OrderResponse;

public interface OrderService {
    long placeOrder(OrderRequest orderRequest);

    OrderResponse getOrderDetails(long orderId);
}
