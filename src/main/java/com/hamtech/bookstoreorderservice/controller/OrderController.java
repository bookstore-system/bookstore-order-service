package com.hamtech.bookstoreorderservice.controller;

import com.hamtech.bookstoreorderservice.model.dto.request.CheckoutRequest;
import com.hamtech.bookstoreorderservice.model.dto.response.ApiResponse;
import com.hamtech.bookstoreorderservice.model.dto.response.OrderResponse;
import com.hamtech.bookstoreorderservice.model.enums.OrderStatus;
import com.hamtech.bookstoreorderservice.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestHeader(value = "X-User-Id", required = false) String userId, 
            @Valid @RequestBody CheckoutRequest request) {
        
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<OrderResponse>builder()
                    .code(4001)
                    .message("Unauthenticated")
                    .build());
        }
        
        OrderResponse orderResponse = orderService.createOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Khởi tạo đơn hàng thành công")
                .result(orderResponse)
                .build());
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<List<OrderResponse>>builder()
                    .code(4001)
                    .message("Unauthenticated")
                    .build());
        }
        
        List<OrderResponse> orders = orderService.getOrdersByUserId(userId);
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đơn hàng thành công")
                .result(orders)
                .build());
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID orderId) {
            
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<OrderResponse>builder()
                    .code(4001)
                    .message("Unauthenticated")
                    .build());
        }
        
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Lấy thông tin đơn hàng thành công")
                .result(orderService.getOrderById(orderId))
                .build());
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @PathVariable UUID orderId) {
            
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<OrderResponse>builder()
                    .code(4001)
                    .message("Unauthenticated")
                    .build());
        }
        
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Hủy đơn hàng thành công")
                .result(orderService.cancelOrder(orderId, userId))
                .build());
    }

    @GetMapping("/admin/all")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100)); // Enforce performance constraint
        return ResponseEntity.ok(ApiResponse.<Page<OrderResponse>>builder()
                .code(1000)
                .result(orderService.getAllOrders(pageable))
                .build());
    }

    // Admin state transition endpoints
    @PostMapping("/admin/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable UUID orderId) {
        return toggleOrderStatus(orderId, OrderStatus.CONFIRMED);
    }
    
    @PostMapping("/admin/{orderId}/process")
    public ResponseEntity<ApiResponse<OrderResponse>> processOrder(@PathVariable UUID orderId) {
        return toggleOrderStatus(orderId, OrderStatus.PROCESSING);
    }

    @PostMapping("/admin/{orderId}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(@PathVariable UUID orderId) {
        return toggleOrderStatus(orderId, OrderStatus.SHIPPED);
    }

    @PostMapping("/admin/{orderId}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(@PathVariable UUID orderId) {
        return toggleOrderStatus(orderId, OrderStatus.DELIVERED);
    }

    private ResponseEntity<ApiResponse<OrderResponse>> toggleOrderStatus(UUID orderId, OrderStatus status) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Cập nhật trạng thái thành công")
                .result(updatedOrder)
                .build());
    }
}
