package com.notfound.orderservice.service.impl;

import com.notfound.orderservice.exception.BusinessException;
import com.notfound.orderservice.exception.ResourceNotFoundException;
import com.notfound.orderservice.model.dto.request.CheckoutRequest;
import com.notfound.orderservice.model.dto.response.OrderResponse;
import com.notfound.orderservice.model.entity.Order;
import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.repository.OrderRepository;
import com.notfound.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;

    @Override
    @Transactional
    public OrderResponse createOrder(String customerId, CheckoutRequest request) {
        log.info("Creating order for userId: {}", customerId);
        
        try {
            Order order = new Order();
            order.setCustomerId(customerId);
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentMethod(request.getPaymentMethod());
            order.setTotalAmount(0.0); // Placeholder. Móc sang Book Client để tính giá.
            
            Order savedOrder = orderRepository.save(order);
            log.info("Order created successfully: orderId={}", savedOrder.getOrderID());
            return mapToResponse(savedOrder);
        } catch (Exception e) {
            log.error("Failed to create order for userId: {}", customerId, e);
            throw new BusinessException("Không thể tạo đơn hàng");
        }
    }

    @Override
    public List<OrderResponse> getOrdersByUserId(String userId) {
        return orderRepository.findByCustomerIdOrderByOrderDateDesc(userId)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public OrderResponse getOrderById(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        return mapToResponse(order);
    }

    @Override
    @Transactional
    public OrderResponse cancelOrder(UUID orderId, String userId) {
        log.info("Request to cancel orderId: {} by userId: {}", orderId, userId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
                
        if (!order.getCustomerId().equals(userId)) {
            throw new BusinessException("Unauthorized to cancel this order");
        }
        
        if (order.getStatus() != OrderStatus.PENDING) {
            throw new BusinessException("Only PENDING orders can be cancelled");
        }
        
        order.setStatus(OrderStatus.CANCELLED);
        log.info("Order {} successfully cancelled", orderId);
        return mapToResponse(orderRepository.save(order));
    }

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        return orderRepository.findAllByOrderByOrderDateDesc(pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus status) {
        log.info("Updating orderId {} status to {}", orderId, status);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId.toString()));
        order.setStatus(status);
        return mapToResponse(orderRepository.save(order));
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    private OrderResponse mapToResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getOrderID())
                .orderDate(order.getOrderDate())
                .status(order.getStatus().name())
                .total(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .customerId(order.getCustomerId())
                .build();
    }
}
