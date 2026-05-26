package com.notfound.orderservice.service.impl;

import com.notfound.orderservice.client.BookClient;
import com.notfound.orderservice.client.PromotionClient;
import com.notfound.orderservice.client.UserClient;
import com.notfound.orderservice.client.dto.AddressResponse;
import com.notfound.orderservice.client.dto.BookDetailResponse;
import com.notfound.orderservice.client.dto.PromotionApplyResponse;
import com.notfound.orderservice.exception.BusinessException;
import com.notfound.orderservice.exception.ResourceNotFoundException;
import com.notfound.orderservice.messaging.OrderEventProducer;
import com.notfound.orderservice.model.dto.request.CheckoutRequest;
import com.notfound.orderservice.model.dto.response.ApiResponse;
import com.notfound.orderservice.model.dto.response.OrderResponse;
import com.notfound.orderservice.model.dto.response.UserOrderSummaryResponse;
import com.notfound.orderservice.model.entity.Order;
import com.notfound.orderservice.model.entity.OrderItem;
import com.notfound.orderservice.model.entity.ShippingDetails;
import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.repository.OrderItemRepository;
import com.notfound.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.argThat;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private BookClient bookClient;
    @Mock private UserClient userClient;
    @Mock private PromotionClient promotionClient;
    @Mock private OrderEventProducer orderEventProducer;

    @InjectMocks private OrderServiceImpl orderService;

    private static final String USER_ID = "user-123";
    private static final UUID ORDER_ID = UUID.randomUUID();
    private static final String BOOK_ID = "book-001";
    private static final String ADDRESS_ID = "addr-001";

    private CheckoutRequest validRequest;
    private Order savedOrder;
    private AddressResponse addressResponse;
    private BookDetailResponse bookDetail;

    @BeforeEach
    void setUp() {
        validRequest = CheckoutRequest.builder()
                .paymentMethod("COD")
                .addressId(ADDRESS_ID)
                .bookIds(List.of(BOOK_ID))
                .build();

        addressResponse = AddressResponse.builder()
                .id(ADDRESS_ID)
                .recipientName("Nguyen Van A")
                .phoneNumber("0123456789")
                .fullAddress("123 Nguyen Trai")
                .province("HCM")
                .district("Q1")
                .ward("P1")
                .build();

        bookDetail = BookDetailResponse.builder()
                .bookId(BOOK_ID)
                .title("Clean Code")
                .price(new BigDecimal("100000"))
                .salePrice(new BigDecimal("90000"))
                .stockQuantity(10)
                .build();

        ShippingDetails shipping = ShippingDetails.builder()
                .recipientName("Nguyen Van A")
                .phoneNumber("0123456789")
                .fullAddress("123 Nguyen Trai")
                .build();

        savedOrder = Order.builder()
                .orderID(ORDER_ID)
                .customerId(USER_ID)
                .status(OrderStatus.PENDING)
                .paymentMethod("COD")
                .totalAmount(90000.0)
                .taxAmount(4500.0)
                .discountAmount(0.0)
                .orderDate(LocalDateTime.now())
                .shippingDetails(shipping)
                .build();
    }

    /*
     * Legacy createOrder tests removed with synchronous checkout flow.
     * Saga order creation is covered by OrderCommandConsumerTest.
     *
    // ==================== createOrder ====================

    @Test
    void createOrder_nullRequest_throwsBusinessException() {
        assertThatThrownBy(() -> orderService.createOrder(USER_ID, null))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createOrder_blankPaymentMethod_throwsBusinessException() {
        validRequest.setPaymentMethod("   ");

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("thanh toán");
    }

    @Test
    void createOrder_blankAddressId_throwsBusinessException() {
        validRequest.setAddressId("  ");

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createOrder_emptyBookIds_throwsBusinessException() {
        validRequest.setBookIds(List.of());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("sách");
    }

    @Test
    void createOrder_addressResultNull_throwsBusinessException() {
        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(null).build());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("địa chỉ");
    }

    @Test
    void createOrder_bookDetailsEmpty_throwsBusinessException() {
        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of()).build());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createOrder_bookNotInDetailMap_throwsBusinessException() {
        BookDetailResponse otherBook = BookDetailResponse.builder()
                .bookId("other-book-999")
                .price(new BigDecimal("50000"))
                .stockQuantity(5)
                .build();

        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(otherBook)).build());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining(BOOK_ID);
    }

    @Test
    void createOrder_insufficientStock_throwsBusinessException() {
        bookDetail.setStockQuantity(0);

        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(bookDetail)).build());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("tồn kho");
    }

    @Test
    void createOrder_nullPrice_throwsBusinessException() {
        bookDetail.setPrice(null);
        bookDetail.setSalePrice(null);

        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(bookDetail)).build());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("giá");
    }

    @Test
    void createOrder_happyPath_noDiscount_returnsOrderResponse() {
        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(bookDetail)).build());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of());

        OrderResponse result = orderService.createOrder(USER_ID, validRequest);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getCustomerId()).isEqualTo(USER_ID);
        verify(bookClient).reduceStock(any());
        verify(promotionClient, never()).applyPromotion(any());
    }

    @Test
    void createOrder_withValidDiscount_appliesDiscountAndFinalTotal() {
        validRequest.setDiscountCode("SAVE10K");

        PromotionApplyResponse promoResponse = PromotionApplyResponse.builder()
                .isValid(true)
                .discountAmount(new BigDecimal("10000"))
                .finalTotal(new BigDecimal("80000"))
                .build();

        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(bookDetail)).build());
        when(promotionClient.applyPromotion(any()))
                .thenReturn(ApiResponse.<PromotionApplyResponse>builder().result(promoResponse).build());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of());

        OrderResponse result = orderService.createOrder(USER_ID, validRequest);

        assertThat(result).isNotNull();
        verify(promotionClient).applyPromotion(any());
        verify(bookClient).reduceStock(any());
    }

    @Test
    void createOrder_invalidPromoCode_throwsBusinessException() {
        validRequest.setDiscountCode("INVALID_CODE");

        PromotionApplyResponse promoResponse = PromotionApplyResponse.builder()
                .isValid(false)
                .build();

        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(bookDetail)).build());
        when(promotionClient.applyPromotion(any()))
                .thenReturn(ApiResponse.<PromotionApplyResponse>builder().result(promoResponse).build());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createOrder_promoResponseNull_throwsBusinessException() {
        validRequest.setDiscountCode("SOME_CODE");

        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(bookDetail)).build());
        when(promotionClient.applyPromotion(any()))
                .thenReturn(ApiResponse.<PromotionApplyResponse>builder().result(null).build());

        assertThatThrownBy(() -> orderService.createOrder(USER_ID, validRequest))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createOrder_useSalePrice_whenSalePricePresent() {
        bookDetail.setPrice(new BigDecimal("100000"));
        bookDetail.setSalePrice(new BigDecimal("80000"));

        when(userClient.getUserAddress(USER_ID, ADDRESS_ID))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(addressResponse).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(bookDetail)).build());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            // salePrice=80000 used, not price=100000
            assertThat(o.getTotalAmount()).isLessThan(100000.0);
            return savedOrder;
        });
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of());

        orderService.createOrder(USER_ID, validRequest);

        verify(orderRepository).save(argThat(o -> o.getTotalAmount() < 100000.0));
    }

     */
    // ==================== cancelOrder ====================

    @Test
    void cancelOrder_orderNotFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelOrder_wrongUser_throwsBusinessException() {
        savedOrder.setCustomerId("other-user");
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Unauthorized");
    }

    @Test
    void cancelOrder_statusShipped_throwsBusinessException() {
        savedOrder.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));

        assertThatThrownBy(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("trạng thái");
    }

    @Test
    void cancelOrder_statusPending_setsStatusCancelledAndSaves() {
        savedOrder.setStatus(OrderStatus.PENDING);
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.cancelOrder(ORDER_ID, USER_ID);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
        verify(bookClient, never()).restoreStock(any());
    }

    @Test
    void cancelOrder_statusConfirmed_restoresStockAndCancels() {
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        OrderItem item = new OrderItem(savedOrder, BOOK_ID, 2, 90000.0);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of(item));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.cancelOrder(ORDER_ID, USER_ID);

        verify(bookClient).restoreStock(any());
        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
    }

    @Test
    void cancelOrder_stockRestoreFails_doesNotThrow() {
        savedOrder.setStatus(OrderStatus.CONFIRMED);
        OrderItem item = new OrderItem(savedOrder, BOOK_ID, 1, 90000.0);

        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of(item));
        doThrow(new RuntimeException("network timeout")).when(bookClient).restoreStock(any());
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        assertThatCode(() -> orderService.cancelOrder(ORDER_ID, USER_ID))
                .doesNotThrowAnyException();

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
    }

    // ==================== getOrderById ====================

    @Test
    void getOrderById_notFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrderById(ORDER_ID))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getOrderById_found_returnsOrderResponse() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of());

        OrderResponse result = orderService.getOrderById(ORDER_ID);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(ORDER_ID);
        assertThat(result.getStatus()).isEqualTo(OrderStatus.PENDING.name());
    }

    // ==================== updateOrderStatus ====================

    @Test
    void updateOrderStatus_notFound_throwsResourceNotFoundException() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.updateOrderStatus(ORDER_ID, OrderStatus.CONFIRMED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOrderStatus_success_savesNewStatus() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.findByOrder_OrderID(ORDER_ID)).thenReturn(List.of());

        orderService.updateOrderStatus(ORDER_ID, OrderStatus.CONFIRMED);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
    }

    /*
     * updateOrderStatusByPayment belonged to the legacy payment.completed consumer flow.
     *
    // ==================== updateOrderStatusByPayment ====================

    @Test
    void updateOrderStatusByPayment_confirmed_publishesOrderPlacedEvent() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.updateOrderStatusByPayment(ORDER_ID, OrderStatus.CONFIRMED);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CONFIRMED));
        verify(orderEventProducer).publishOrderPlaced(any());
    }

    @Test
    void updateOrderStatusByPayment_cancelled_doesNotPublishEvent() {
        when(orderRepository.findById(ORDER_ID)).thenReturn(Optional.of(savedOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        orderService.updateOrderStatusByPayment(ORDER_ID, OrderStatus.CANCELLED);

        verify(orderRepository).save(argThat(o -> o.getStatus() == OrderStatus.CANCELLED));
        verify(orderEventProducer, never()).publishOrderPlaced(any());
    }

     */
    // ==================== getTotalRevenue ====================

    @Test
    void getTotalRevenue_nullFromRepo_returnsZero() {
        when(orderRepository.getTotalRevenue(any(), any())).thenReturn(null);

        Double result = orderService.getTotalRevenue(null, null);

        assertThat(result).isEqualTo(0.0);
    }

    @Test
    void getTotalRevenue_withValue_returnsThatValue() {
        when(orderRepository.getTotalRevenue(any(), any())).thenReturn(1500000.0);

        Double result = orderService.getTotalRevenue(null, null);

        assertThat(result).isEqualTo(1500000.0);
    }

    // ==================== getUserSummary ====================

    @Test
    void getUserSummary_emptyMap_returnsDefaultZeroResponse() {
        when(orderRepository.getUserSummary(USER_ID)).thenReturn(Map.of());

        UserOrderSummaryResponse result = orderService.getUserSummary(USER_ID);

        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getTotalOrders()).isEqualTo(0L);
        assertThat(result.getTotalSpent()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void getUserSummary_nullUserIdInMap_returnsDefaultZeroResponse() {
        Map<String, Object> mapWithNullUserId = new HashMap<>();
        mapWithNullUserId.put("userId", null);
        when(orderRepository.getUserSummary(USER_ID)).thenReturn(mapWithNullUserId);

        UserOrderSummaryResponse result = orderService.getUserSummary(USER_ID);

        assertThat(result.getTotalOrders()).isEqualTo(0L);
    }

    // ==================== hasUserPurchasedAndReceivedBook ====================

    @Test
    void hasUserPurchasedAndReceivedBook_delegatesToRepository() {
        when(orderRepository.existsByCustomerIdAndBookIdAndStatus(USER_ID, BOOK_ID, OrderStatus.DELIVERED))
                .thenReturn(true);

        boolean result = orderService.hasUserPurchasedAndReceivedBook(USER_ID, BOOK_ID);

        assertThat(result).isTrue();
        verify(orderRepository).existsByCustomerIdAndBookIdAndStatus(USER_ID, BOOK_ID, OrderStatus.DELIVERED);
    }

    @Test
    void hasUserPurchasedAndReceivedBook_notDelivered_returnsFalse() {
        when(orderRepository.existsByCustomerIdAndBookIdAndStatus(USER_ID, BOOK_ID, OrderStatus.DELIVERED))
                .thenReturn(false);

        boolean result = orderService.hasUserPurchasedAndReceivedBook(USER_ID, BOOK_ID);

        assertThat(result).isFalse();
    }
}
