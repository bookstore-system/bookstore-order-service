package com.notfound.orderservice.service;

import com.notfound.orderservice.model.dto.request.CheckoutRequest;
import com.notfound.orderservice.model.dto.response.OrderResponse;
import com.notfound.orderservice.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    OrderResponse createOrder(String customerId, CheckoutRequest request);
    
    List<OrderResponse> getOrdersByUserId(String userId);

    OrderResponse getOrderById(UUID orderId);

    OrderResponse cancelOrder(UUID orderId, String userId);

    Page<OrderResponse> getAllOrders(Pageable pageable);

    OrderResponse updateOrderStatus(UUID orderId, OrderStatus status);

    List<OrderResponse> getOrdersByStatus(OrderStatus status);
}
