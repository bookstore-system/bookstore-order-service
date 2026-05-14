package com.notfound.orderservice.controller;

import com.notfound.orderservice.client.PaymentClient;
import com.notfound.orderservice.exception.BusinessException;
import com.notfound.orderservice.model.dto.request.CheckoutRequest;
import com.notfound.orderservice.model.dto.request.PaymentRequest;
import com.notfound.orderservice.model.dto.response.AdminStatsResponse;
import com.notfound.orderservice.model.dto.response.ApiResponse;
import com.notfound.orderservice.model.dto.response.CheckPurchasedResponse;
import com.notfound.orderservice.model.dto.response.CreatePaymentResponse;
import com.notfound.orderservice.model.dto.response.OrderResponse;
import com.notfound.orderservice.model.dto.response.StatsResponse;
import com.notfound.orderservice.model.dto.response.UserOrderSummaryResponse;
import com.notfound.orderservice.model.dto.response.UserSpenderResponse;
import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final PaymentClient paymentClient;

    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @RequestHeader(value = "X-User-Id", required = false) String userId, 
            @RequestBody(required = false) CheckoutRequest request) {
        
        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<OrderResponse>builder()
                    .code(4001)
                    .message("Unauthenticated")
                    .build());
        }
        
        if (request == null) {
            request = CheckoutRequest.builder()
                .paymentMethod("COD")
                .build();
        }
        if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
            request.setPaymentMethod("COD");
        }

        OrderResponse orderResponse = orderService.createOrder(userId, request);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Khởi tạo đơn hàng thành công")
                .result(orderResponse)
                .build());
    }

        @PostMapping("/checkout/vnpay")
        public ResponseEntity<ApiResponse<CreatePaymentResponse>> checkoutWithVNPay(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody CheckoutRequest request) {

        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<CreatePaymentResponse>builder()
                .code(4001)
                .message("Unauthenticated")
                .build());
        }

        if (request == null) {
            throw new BusinessException("Dữ liệu tạo đơn không hợp lệ");
        }

        request.setPaymentMethod("VNPay");
        OrderResponse orderResponse = orderService.createOrder(userId, request);

        PaymentRequest paymentRequest = PaymentRequest.builder()
            .orderId(orderResponse.getId())
            .amount(orderResponse.getTotal() != null ? orderResponse.getTotal().longValue() : 0L)
            .build();

        ApiResponse<CreatePaymentResponse> paymentResponse = paymentClient.createVNPayPayment(paymentRequest);
        CreatePaymentResponse payment = paymentResponse != null ? paymentResponse.getResult() : null;
        if (payment == null) {
            throw new BusinessException("Không thể tạo thanh toán VNPay");
        }

        return ResponseEntity.ok(ApiResponse.<CreatePaymentResponse>builder()
            .code(1000)
            .message("Đã tạo đơn hàng và đường dẫn thanh toán VNPay thành công")
            .result(payment)
            .build());
        }

        @PostMapping("/checkout/zalopay")
        public ResponseEntity<ApiResponse<CreatePaymentResponse>> checkoutWithZaloPay(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody CheckoutRequest request) {

        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<CreatePaymentResponse>builder()
                .code(4001)
                .message("Unauthenticated")
                .build());
        }

        if (request == null) {
            throw new BusinessException("Dữ liệu tạo đơn không hợp lệ");
        }

        request.setPaymentMethod("ZaloPay");
        OrderResponse orderResponse = orderService.createOrder(userId, request);

        PaymentRequest paymentRequest = PaymentRequest.builder()
            .orderId(orderResponse.getId())
            .amount(orderResponse.getTotal() != null ? orderResponse.getTotal().longValue() : 0L)
            .build();

        ApiResponse<CreatePaymentResponse> paymentResponse = paymentClient.createZaloPayPayment(paymentRequest);
        CreatePaymentResponse payment = paymentResponse != null ? paymentResponse.getResult() : null;
        if (payment == null) {
            throw new BusinessException("Không thể tạo thanh toán ZaloPay");
        }

        return ResponseEntity.ok(ApiResponse.<CreatePaymentResponse>builder()
            .code(1000)
            .message("Đã tạo đơn hàng và đường dẫn thanh toán ZaloPay thành công")
            .result(payment)
            .build());
        }

        @PostMapping("/checkout/momo")
        public ResponseEntity<ApiResponse<CreatePaymentResponse>> checkoutWithMoMo(
            @RequestHeader(value = "X-User-Id", required = false) String userId,
            @RequestBody CheckoutRequest request) {

        if (userId == null) {
            return ResponseEntity.status(401).body(ApiResponse.<CreatePaymentResponse>builder()
                .code(4001)
                .message("Unauthenticated")
                .build());
        }

        if (request == null) {
            throw new BusinessException("Dữ liệu tạo đơn không hợp lệ");
        }

        request.setPaymentMethod("MoMo");
        OrderResponse orderResponse = orderService.createOrder(userId, request);

        PaymentRequest paymentRequest = PaymentRequest.builder()
            .orderId(orderResponse.getId())
            .amount(orderResponse.getTotal() != null ? orderResponse.getTotal().longValue() : 0L)
            .build();

        ApiResponse<CreatePaymentResponse> paymentResponse = paymentClient.createMoMoPayment(paymentRequest);
        CreatePaymentResponse payment = paymentResponse != null ? paymentResponse.getResult() : null;
        if (payment == null) {
            throw new BusinessException("Không thể tạo thanh toán MoMo");
        }

        return ResponseEntity.ok(ApiResponse.<CreatePaymentResponse>builder()
            .code(1000)
            .message("Đã tạo đơn hàng và đường dẫn thanh toán MoMo thành công")
            .result(payment)
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

    @PutMapping("/admin/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable UUID orderId,
            @RequestParam String status) {

        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            OrderResponse order = orderService.updateOrderStatus(orderId, orderStatus);

            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(1000)
                    .message("Cập nhật trạng thái đơn hàng thành công")
                    .result(order)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003)
                    .message("Trạng thái không hợp lệ. Các trạng thái: PENDING, CONFIRMED, PROCESSING, SHIPPED, DELIVERED, CANCELLED, COMPLETED")
                    .build());
        }
    }

    // Admin state transition endpoints
    @PostMapping("/admin/{orderId}/confirm")
    public ResponseEntity<ApiResponse<OrderResponse>> confirmOrder(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        if (!"COD".equalsIgnoreCase(order.getPaymentMethod())) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003).message("Chỉ có thể xác nhận đơn hàng COD").build());
        }
        if (!"PENDING".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003).message("Chỉ có thể xác nhận đơn hàng đang ở trạng thái PENDING").build());
        }
        return toggleOrderStatus(orderId, OrderStatus.CONFIRMED, "Xác nhận đơn hàng COD thành công");
    }

    @PostMapping("/admin/{orderId}/process")
    public ResponseEntity<ApiResponse<OrderResponse>> processOrder(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        if (!"CONFIRMED".equals(order.getStatus()) && !"PENDING".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003).message("Chỉ có thể xử lý đơn hàng đang ở trạng thái CONFIRMED hoặc PENDING").build());
        }
        return toggleOrderStatus(orderId, OrderStatus.PROCESSING, "Bắt đầu xử lý đơn hàng thành công");
    }

    @PostMapping("/admin/{orderId}/ship")
    public ResponseEntity<ApiResponse<OrderResponse>> shipOrder(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        if (!"PROCESSING".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003).message("Chỉ có thể giao hàng khi đơn đang ở trạng thái PROCESSING").build());
        }
        return toggleOrderStatus(orderId, OrderStatus.SHIPPED, "Đơn hàng đã được giao cho shipper");
    }

    @PostMapping("/admin/{orderId}/deliver")
    public ResponseEntity<ApiResponse<OrderResponse>> deliverOrder(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        if (!"SHIPPED".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003).message("Chỉ có thể xác nhận giao hàng khi đơn đang ở trạng thái SHIPPED").build());
        }
        return toggleOrderStatus(orderId, OrderStatus.DELIVERED, "Đơn hàng đã được giao thành công");
    }

    @PostMapping("/admin/{orderId}/complete")
    public ResponseEntity<ApiResponse<OrderResponse>> completeOrder(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        if (!"DELIVERED".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003).message("Chỉ có thể hoàn thành đơn hàng đã được giao").build());
        }
        return toggleOrderStatus(orderId, OrderStatus.COMPLETED, "Đơn hàng đã hoàn thành");
    }

    @PostMapping("/admin/{orderId}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> adminCancelOrder(@PathVariable UUID orderId) {
        OrderResponse order = orderService.getOrderById(orderId);
        if ("DELIVERED".equals(order.getStatus()) || "COMPLETED".equals(order.getStatus())) {
            return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                    .code(4003).message("Không thể hủy đơn hàng đã giao hoặc đã hoàn thành").build());
        }
        return toggleOrderStatus(orderId, OrderStatus.CANCELLED, "Đã hủy đơn hàng");
    }

    @GetMapping("/admin/{orderId}/details")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderDetailsAdmin(@PathVariable UUID orderId) {
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message("Lấy chi tiết đơn hàng thành công")
                .result(orderService.getOrderById(orderId))
                .build());
    }

    @GetMapping("/admin/cod/pending")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getPendingCODOrders() {
        List<OrderResponse> codOrders = orderService.getOrdersByStatus(OrderStatus.PENDING).stream()
                .filter(order -> "COD".equalsIgnoreCase(order.getPaymentMethod()))
                .toList();
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .code(1000)
                .message("Lấy danh sách đơn COD chưa xác nhận thành công")
                .result(codOrders)
                .build());
    }

    @GetMapping("/admin/search")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> searchOrders(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        List<OrderResponse> filtered = orderService.getAllOrders(pageable).getContent().stream()
                .filter(order ->
                    order.getId().toString().contains(keyword) ||
                    (order.getRecipientName() != null &&
                     order.getRecipientName().toLowerCase().contains(keyword.toLowerCase())) ||
                    (order.getCustomerName() != null &&
                     order.getCustomerName().toLowerCase().contains(keyword.toLowerCase())))
                .toList();
        return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                .code(1000)
                .message("Tìm kiếm đơn hàng thành công")
                .result(filtered)
                .build());
    }

    private ResponseEntity<ApiResponse<OrderResponse>> toggleOrderStatus(UUID orderId, OrderStatus status, String message) {
        OrderResponse updatedOrder = orderService.updateOrderStatus(orderId, status);
        return ResponseEntity.ok(ApiResponse.<OrderResponse>builder()
                .code(1000)
                .message(message)
                .result(updatedOrder)
                .build());
    }

    // Statistics Endpoints
    @GetMapping("/stats")
    public ResponseEntity<StatsResponse> getGlobalStats(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }
        
        return ResponseEntity.ok(orderService.getGlobalStats(startDate, endDate));
    }

    @GetMapping("/admin/stats")
    public ResponseEntity<AdminStatsResponse> getAdminStats() {
        return ResponseEntity.ok(orderService.getAdminAiStats());
    }

    @GetMapping("/admin/status/{status}")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getOrdersByStatus(
            @PathVariable String status,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            OrderStatus orderStatus = OrderStatus.valueOf(status.toUpperCase());
            List<OrderResponse> orders;

            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                orders = orderService.getOrdersByStatus(orderStatus, start, end);
            } else {
                orders = orderService.getOrdersByStatus(orderStatus);
            }

            return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                    .code(1000)
                    .message("Lấy danh sách đơn hàng theo trạng thái thành công")
                    .result(orders)
                    .build());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                    .code(4003)
                    .message("Trạng thái không hợp lệ")
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<List<OrderResponse>>builder()
                    .code(4004)
                    .message("Lỗi định dạng ngày tháng: " + e.getMessage())
                    .build());
        }
    }

    @GetMapping("/admin/revenue")
    public ResponseEntity<ApiResponse<Double>> getTotalRevenue(
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        try {
            Double revenue;

            if (startDate != null && endDate != null) {
                LocalDateTime start = LocalDate.parse(startDate).atStartOfDay();
                LocalDateTime end = LocalDate.parse(endDate).atTime(23, 59, 59);
                revenue = orderService.getTotalRevenue(start, end);
            } else {
                revenue = orderService.getTotalRevenue();
            }

            return ResponseEntity.ok(ApiResponse.<Double>builder()
                    .code(1000)
                    .message("Lấy tổng doanh thu thành công")
                    .result(revenue)
                    .build());
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.<Double>builder()
                    .code(4004)
                    .message("Lỗi định dạng ngày tháng: " + e.getMessage())
                    .result(0.0)
                    .build());
        }
    }

    @GetMapping("/top-spenders")
    public ResponseEntity<List<UserSpenderResponse>> getTopSpenders(
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(orderService.getTopSpenders(limit));
    }

    @GetMapping("/top-buyers")
    public ResponseEntity<List<UserSpenderResponse>> getTopBuyers(
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(orderService.getTopBuyers(limit));
    }

    @GetMapping("/users/{userId}/summary")
    public ResponseEntity<UserOrderSummaryResponse> getUserSummary(@PathVariable String userId) {
        return ResponseEntity.ok(orderService.getUserSummary(userId));
    }

    @GetMapping("/check-purchased")
    public ResponseEntity<CheckPurchasedResponse> checkPurchased(
            @RequestParam String userId,
            @RequestParam String bookId) {
        boolean isPurchased = orderService.hasUserPurchasedAndReceivedBook(userId, bookId);
        
        String message = isPurchased 
                ? "User has successfully purchased and received this book." 
                : "User has not purchased or received this book yet.";
                
        return ResponseEntity.ok(CheckPurchasedResponse.builder()
                .isPurchased(isPurchased)
                .message(message)
                .build());
    }

    @GetMapping("/count")
    public ResponseEntity<ApiResponse<Long>> countMyOrders(
            @RequestHeader(value = "X-User-Id", required = false) String userId) {
        if (userId == null) {
            return ResponseEntity.ok(ApiResponse.<Long>builder()
                    .code(1000)
                    .result(0L)
                    .build());
        }

        Long count = orderService.countOrdersByUserId(userId);

        return ResponseEntity.ok(ApiResponse.<Long>builder()
                .code(1000)
                .message("Đếm số đơn hàng thành công")
                .result(count)
                .build());
    }
}
