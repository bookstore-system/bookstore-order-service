package com.notfound.orderservice.controller;

import com.notfound.orderservice.exception.GlobalExceptionHandler;
import com.notfound.orderservice.exception.BusinessException;
import com.notfound.orderservice.exception.ResourceNotFoundException;
import com.notfound.orderservice.model.dto.response.OrderResponse;
import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock private OrderService orderService;
    private MockMvc mockMvc;

    private static final String USER_ID = "user-123";
    private static final UUID ORDER_ID = UUID.randomUUID();

    private OrderResponse sampleOrderResponse;

    @BeforeEach
    void setUp() {
        OrderController controller = new OrderController(orderService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        sampleOrderResponse = OrderResponse.builder()
                .id(ORDER_ID)
                .status(OrderStatus.PENDING.name())
                .customerId(USER_ID)
                .paymentMethod("COD")
                .total(90000.0)
                .build();
    }

    @Test
    void legacyCheckoutEndpoint_returns410() throws Exception {
        mockMvc.perform(post("/api/v1/orders/checkout")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value(410));
    }

    // ==================== GET /api/v1/orders ====================

    @Test
    void getMyOrders_missingUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(4001));
    }

    @Test
    void getMyOrders_validUserId_returns200WithList() throws Exception {
        when(orderService.getOrdersByUserId(USER_ID)).thenReturn(List.of(sampleOrderResponse));

        mockMvc.perform(get("/api/v1/orders")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result[0].id").value(ORDER_ID.toString()));
    }

    // ==================== GET /api/v1/orders/{orderId} ====================

    @Test
    void getOrderById_missingUserId_returns401() throws Exception {
        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getOrderById_validRequest_returns200() throws Exception {
        when(orderService.getOrderById(ORDER_ID)).thenReturn(sampleOrderResponse);

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.id").value(ORDER_ID.toString()));
    }

    @Test
    void getOrderById_orderNotFound_returns404() throws Exception {
        when(orderService.getOrderById(ORDER_ID))
                .thenThrow(new ResourceNotFoundException("Order", ORDER_ID.toString()));

        mockMvc.perform(get("/api/v1/orders/{orderId}", ORDER_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404));
    }

    // ==================== POST /api/v1/orders/{orderId}/cancel ====================

    @Test
    void cancelOrder_missingUserId_returns401() throws Exception {
        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelOrder_validRequest_returns200() throws Exception {
        OrderResponse cancelledOrder = OrderResponse.builder()
                .id(ORDER_ID)
                .status(OrderStatus.CANCELLED.name())
                .build();
        when(orderService.cancelOrder(ORDER_ID, USER_ID)).thenReturn(cancelledOrder);

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_businessException_returns400() throws Exception {
        when(orderService.cancelOrder(ORDER_ID, USER_ID))
                .thenThrow(new BusinessException("Không thể hủy đơn hàng"));

        mockMvc.perform(post("/api/v1/orders/{orderId}/cancel", ORDER_ID)
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    // ==================== PUT /api/v1/orders/admin/{orderId}/status ====================

    @Test
    void updateOrderStatus_invalidStatus_returns200WithCode4003() throws Exception {
        mockMvc.perform(put("/api/v1/orders/admin/{orderId}/status", ORDER_ID)
                        .param("status", "INVALID_STATUS"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void updateOrderStatus_validStatus_returns200() throws Exception {
        when(orderService.updateOrderStatus(ORDER_ID, OrderStatus.CONFIRMED)).thenReturn(sampleOrderResponse);

        mockMvc.perform(put("/api/v1/orders/admin/{orderId}/status", ORDER_ID)
                        .param("status", "CONFIRMED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000));
    }

    // ==================== GET /api/v1/orders/stats ====================

    @Test
    void getGlobalStats_startDateAfterEndDate_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/orders/stats")
                        .param("startDate", "2025-12-31T00:00:00")
                        .param("endDate", "2025-01-01T00:00:00"))
                .andExpect(status().isBadRequest());
    }

    // ==================== GET /api/v1/orders/count ====================

    @Test
    void countMyOrders_nullUserId_returns200WithZero() throws Exception {
        mockMvc.perform(get("/api/v1/orders/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(0));
    }

    @Test
    void countMyOrders_validUserId_returns200WithCount() throws Exception {
        when(orderService.countOrdersByUserId(USER_ID)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/orders/count")
                        .header("X-User-Id", USER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result").value(5));
    }

    // ==================== GET /api/v1/orders/check-purchased ====================

    @Test
    void checkPurchased_returnsTrueWhenPurchased() throws Exception {
        when(orderService.hasUserPurchasedAndReceivedBook(USER_ID, "book-001")).thenReturn(true);

        mockMvc.perform(get("/api/v1/orders/check-purchased")
                        .param("userId", USER_ID)
                        .param("bookId", "book-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.purchased").value(true));
    }

    // ==================== POST /api/v1/orders/admin/{orderId}/confirm ====================

    @Test
    void confirmOrder_nonCodPayment_returns200WithCode4003() throws Exception {
        OrderResponse vnpayOrder = OrderResponse.builder()
                .id(ORDER_ID)
                .status("PENDING")
                .paymentMethod("VNPay")
                .build();
        when(orderService.getOrderById(ORDER_ID)).thenReturn(vnpayOrder);

        mockMvc.perform(post("/api/v1/orders/admin/{orderId}/confirm", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(4003));
    }

    @Test
    void confirmOrder_codPendingOrder_returns200Confirmed() throws Exception {
        OrderResponse pendingCodOrder = OrderResponse.builder()
                .id(ORDER_ID)
                .status("PENDING")
                .paymentMethod("COD")
                .build();
        OrderResponse confirmedOrder = OrderResponse.builder()
                .id(ORDER_ID)
                .status("CONFIRMED")
                .paymentMethod("COD")
                .build();

        when(orderService.getOrderById(ORDER_ID)).thenReturn(pendingCodOrder);
        when(orderService.updateOrderStatus(ORDER_ID, OrderStatus.CONFIRMED)).thenReturn(confirmedOrder);

        mockMvc.perform(post("/api/v1/orders/admin/{orderId}/confirm", ORDER_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1000))
                .andExpect(jsonPath("$.result.status").value("CONFIRMED"));
    }
}
