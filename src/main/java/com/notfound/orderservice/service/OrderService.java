package com.notfound.orderservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.notfound.orderservice.model.dto.request.CheckoutRequest;
import com.notfound.orderservice.model.dto.response.AdminStatsResponse;
import com.notfound.orderservice.model.dto.response.OrderResponse;
import com.notfound.orderservice.model.dto.response.StatsResponse;
import com.notfound.orderservice.model.dto.response.UserOrderSummaryResponse;
import com.notfound.orderservice.model.dto.response.UserSpenderResponse;
import com.notfound.orderservice.model.enums.OrderStatus;

public interface OrderService {
    OrderResponse createOrder(String customerId, CheckoutRequest request);
    
    List<OrderResponse> getOrdersByUserId(String userId);

    OrderResponse getOrderById(UUID orderId);

    OrderResponse cancelOrder(UUID orderId, String userId);

    Page<OrderResponse> getAllOrders(Pageable pageable);

    OrderResponse updateOrderStatus(UUID orderId, OrderStatus status);

    List<OrderResponse> getOrdersByStatus(OrderStatus status);

    List<OrderResponse> getOrdersByStatus(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate);

    Double getTotalRevenue();

    Double getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate);

    StatsResponse getGlobalStats(LocalDateTime startDate, LocalDateTime endDate);

    List<UserSpenderResponse> getTopSpenders(int limit);

    List<UserSpenderResponse> getTopBuyers(int limit);

    UserOrderSummaryResponse getUserSummary(String userId);

    Long countOrdersByUserId(String userId);

    AdminStatsResponse getAdminAiStats();

    boolean hasUserPurchasedAndReceivedBook(String userId, String bookId);

    void updateOrderStatusByPayment(java.util.UUID orderId, OrderStatus newStatus);
}
