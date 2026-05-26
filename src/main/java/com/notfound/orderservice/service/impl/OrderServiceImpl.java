package com.notfound.orderservice.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.notfound.orderservice.client.BookClient;
import com.notfound.orderservice.client.dto.ReduceStockItem;
import com.notfound.orderservice.client.dto.ReduceStockRequest;
import com.notfound.orderservice.exception.BusinessException;
import com.notfound.orderservice.exception.ResourceNotFoundException;
import com.notfound.orderservice.messaging.OrderEventProducer;
import com.notfound.orderservice.model.dto.response.AdminStatsResponse;
import com.notfound.orderservice.model.dto.response.OrderItemResponse;
import com.notfound.orderservice.model.dto.response.OrderResponse;
import com.notfound.orderservice.model.dto.response.StatsResponse;
import com.notfound.orderservice.model.dto.response.UserOrderSummaryResponse;
import com.notfound.orderservice.model.dto.response.UserSpenderResponse;
import com.notfound.orderservice.model.entity.Order;
import com.notfound.orderservice.model.entity.OrderItem;
import com.notfound.orderservice.model.entity.ShippingDetails;
import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.repository.OrderItemRepository;
import com.notfound.orderservice.repository.OrderRepository;
import com.notfound.orderservice.service.OrderService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final BookClient bookClient;
    private final OrderEventProducer orderEventProducer;

    /*
     * Legacy synchronous checkout was removed from the public flow.
     * New checkout orders are created by OrderCommandConsumer through the saga contract.
     *
    @Override
    @Transactional
    public OrderResponse createOrder(String customerId, CheckoutRequest request) {
        log.info("Creating order for userId: {}", customerId);

        try {
            if (request == null) {
                throw new BusinessException("Dữ liệu tạo đơn không hợp lệ");
            }
            if (request.getPaymentMethod() == null || request.getPaymentMethod().isBlank()) {
                throw new BusinessException("Phương thức thanh toán không hợp lệ");
            }
            if (request.getAddressId() == null || request.getAddressId().isBlank()) {
                throw new BusinessException("Địa chỉ giao hàng không hợp lệ");
            }
            if (request.getBookIds() == null || request.getBookIds().isEmpty()) {
                throw new BusinessException("Danh sách sách cần đặt không hợp lệ");
            }

            List<String> normalizedBookIds = request.getBookIds().stream()
                    .filter(bookId -> bookId != null && !bookId.isBlank())
                    .collect(Collectors.toList());
            if (normalizedBookIds.isEmpty()) {
                throw new BusinessException("Danh sách sách cần đặt không hợp lệ");
            }

            ApiResponse<AddressResponse> addressResponse = userClient.getUserAddress(customerId, request.getAddressId());
            AddressResponse address = addressResponse != null ? addressResponse.getResult() : null;
            if (address == null) {
                throw new BusinessException("Không tìm thấy địa chỉ giao hàng");
            }

            ShippingDetails shippingDetails = ShippingDetails.builder()
                    .recipientName(address.getRecipientName())
                    .phoneNumber(address.getPhoneNumber())
                    .fullAddress(address.getFullAddress())
                    .province(address.getProvince())
                    .district(address.getDistrict())
                    .ward(address.getWard())
                    .shippingNote(request.getNote())
                    .build();

            Map<String, Long> bookQuantities = normalizedBookIds.stream()
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            List<String> uniqueBookIds = new ArrayList<>(bookQuantities.keySet());

            ApiResponse<List<BookDetailResponse>> bookResponse = bookClient.getBatchBookDetails(
                    BookBatchRequest.builder().bookIds(uniqueBookIds).build()
            );
            List<BookDetailResponse> bookDetails = bookResponse != null ? bookResponse.getResult() : null;
            if (bookDetails == null || bookDetails.isEmpty()) {
                throw new BusinessException("Không lấy được thông tin sách từ hệ thống");
            }

            Map<String, BookDetailResponse> detailMap = bookDetails.stream()
                    .filter(detail -> detail.getBookId() != null)
                    .collect(Collectors.toMap(BookDetailResponse::getBookId, Function.identity(), (a, b) -> a));

            Order order = new Order();
            order.setCustomerId(customerId);
            order.setStatus(OrderStatus.PENDING);
            order.setPaymentMethod(request.getPaymentMethod());
            order.setShippingDetails(shippingDetails);

            List<OrderItem> orderItems = new ArrayList<>();
            BigDecimal subtotal = BigDecimal.ZERO;

            for (Map.Entry<String, Long> entry : bookQuantities.entrySet()) {
                String bookId = entry.getKey();
                int quantity = Math.toIntExact(entry.getValue());
                BookDetailResponse detail = detailMap.get(bookId);

                if (detail == null) {
                    throw new BusinessException("Không tìm thấy thông tin sách: " + bookId);
                }
                if (detail.getStockQuantity() == null || detail.getStockQuantity() < quantity) {
                    throw new BusinessException("Sách không đủ tồn kho: " + bookId);
                }

                BigDecimal unitPrice = detail.getSalePrice() != null ? detail.getSalePrice() : detail.getPrice();
                if (unitPrice == null) {
                    throw new BusinessException("Không tìm thấy giá cho sách: " + bookId);
                }

                subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
                orderItems.add(new OrderItem(order, bookId, quantity, unitPrice.doubleValue()));
            }

            BigDecimal discountAmount = BigDecimal.ZERO;
            BigDecimal finalTotal = subtotal;

            if (request.getDiscountCode() != null && !request.getDiscountCode().isBlank()) {
                PromotionApplyRequest promotionRequest = PromotionApplyRequest.builder()
                        .code(request.getDiscountCode())
                        .userId(customerId)
                        .orderTotalBeforeDiscount(subtotal)
                        .build();

                ApiResponse<PromotionApplyResponse> promotionResponse = promotionClient.applyPromotion(promotionRequest);
                PromotionApplyResponse promotion = promotionResponse != null ? promotionResponse.getResult() : null;

                if (promotion == null) {
                    throw new BusinessException("Không thể áp dụng mã giảm giá");
                }
                if (!promotion.isValid()) {
                    throw new BusinessException("Mã giảm giá không hợp lệ");
                }

                if (promotion.getDiscountAmount() != null) {
                    discountAmount = promotion.getDiscountAmount();
                }
                if (promotion.getFinalTotal() != null) {
                    finalTotal = promotion.getFinalTotal();
                } else {
                    finalTotal = subtotal.subtract(discountAmount);
                }

                order.setPromotionId(request.getDiscountCode());
            }

            if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
                finalTotal = BigDecimal.ZERO;
            }

            double taxAmount = finalTotal.doubleValue() * TAX_RATE;

            order.setDiscountAmount(discountAmount.doubleValue());
            order.setTaxAmount(taxAmount);
            order.setTotalAmount(finalTotal.doubleValue());
            order.setOrderItems(orderItems);

            Order savedOrder = orderRepository.save(order);

            ReduceStockRequest reduceStockRequest = ReduceStockRequest.builder()
                    .items(bookQuantities.entrySet().stream()
                            .map(entry -> ReduceStockItem.builder()
                                    .bookId(entry.getKey())
                                    .quantity(Math.toIntExact(entry.getValue()))
                                    .build())
                            .collect(Collectors.toList()))
                    .build();

            bookClient.reduceStock(reduceStockRequest);

            log.info("Order created successfully: orderId={}", savedOrder.getOrderID());
            return mapToResponse(savedOrder);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Failed to create order for userId: {}", customerId, e);
            throw new BusinessException("Không thể tạo đơn hàng");
        }
    }

     */
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

        if (order.getStatus() != OrderStatus.PENDING && order.getStatus() != OrderStatus.CONFIRMED) {
            throw new BusinessException("Không thể hủy đơn hàng ở trạng thái hiện tại");
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrder_OrderID(orderId);
        if (!orderItems.isEmpty()) {
            ReduceStockRequest restoreRequest = ReduceStockRequest.builder()
                    .items(orderItems.stream()
                            .map(item -> ReduceStockItem.builder()
                                    .bookId(item.getBookId())
                                    .quantity(item.getQuantity())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
            try {
                bookClient.restoreStock(restoreRequest);
            } catch (Exception e) {
                log.warn("Failed to restore stock for cancelled order {}: {}", orderId, e.getMessage());
            }
        }

        order.setStatus(OrderStatus.CANCELLED);
        Order cancelledOrder = orderRepository.save(order);
        log.info("Order {} successfully cancelled", orderId);

        try {
            orderEventProducer.publishOrderCancelled(
                com.notfound.orderservice.messaging.OrderPlacedEvent.builder()
                    .orderId(cancelledOrder.getOrderID())
                    .userId(cancelledOrder.getCustomerId())
                    .totalAmount(cancelledOrder.getTotalAmount())
                    .paymentMethod(cancelledOrder.getPaymentMethod())
                    .createdAt(cancelledOrder.getOrderDate())
                    .build()
            );
        } catch (Exception e) {
            log.warn("Publish order.cancelled fail orderId={}: {}", orderId, e.getMessage());
        }

        return mapToResponse(cancelledOrder);
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
        OrderStatus previous = order.getStatus();
        order.setStatus(status);
        Order saved = orderRepository.save(order);

        if (status == OrderStatus.CANCELLED && previous != OrderStatus.CANCELLED) {
            try {
                orderEventProducer.publishOrderCancelled(
                    com.notfound.orderservice.messaging.OrderPlacedEvent.builder()
                        .orderId(saved.getOrderID())
                        .userId(saved.getCustomerId())
                        .totalAmount(saved.getTotalAmount())
                        .paymentMethod(saved.getPaymentMethod())
                        .createdAt(saved.getOrderDate())
                        .build()
                );
            } catch (Exception e) {
                log.warn("Publish order.cancelled fail orderId={}: {}", orderId, e.getMessage());
            }
        }

        return mapToResponse(saved);
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status) {
        return orderRepository.findByStatus(status)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<OrderResponse> getOrdersByStatus(OrderStatus status, LocalDateTime startDate, LocalDateTime endDate) {
        return orderRepository.findByStatusAndDateRange(status, startDate, endDate)
                .stream().map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public Double getTotalRevenue() {
        return getTotalRevenue(null, null);
    }

    @Override
    public Double getTotalRevenue(LocalDateTime startDate, LocalDateTime endDate) {
        Double revenue = orderRepository.getTotalRevenue(startDate, endDate);
        return revenue != null ? revenue : 0.0;
    }

    @Override
    public StatsResponse getGlobalStats(LocalDateTime startDate, LocalDateTime endDate) {
        Map<String, Object> stats = orderRepository.getGlobalStats(startDate, endDate);
        
        Long totalOrders = (Long) stats.getOrDefault("totalOrders", 0L);
        Double totalRevenueD = (Double) stats.get("totalRevenue");
        Double avgRevenuePerUserD = (Double) stats.get("avgRevenuePerUser");
        Double avgOrderValueD = (Double) stats.get("avgOrderValue");

        return StatsResponse.builder()
                .totalRevenue(totalRevenueD != null ? BigDecimal.valueOf(totalRevenueD) : BigDecimal.ZERO)
                .totalOrders(totalOrders)
                .avgRevenuePerUser(avgRevenuePerUserD != null ? BigDecimal.valueOf(avgRevenuePerUserD) : BigDecimal.ZERO)
                .avgOrderValue(avgOrderValueD != null ? BigDecimal.valueOf(avgOrderValueD) : BigDecimal.ZERO)
                .currency("VND")
                .build();
    }

    @Override
    public List<UserSpenderResponse> getTopSpenders(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 100)));
        Page<Map<String, Object>> results = orderRepository.getTopSpenders(pageable);
        
        return results.getContent().stream().map(row -> UserSpenderResponse.builder()
                .userId((String) row.get("userId"))
                .totalOrders(((Long) row.get("totalOrders")))
                .totalSpent(BigDecimal.valueOf((Double) row.get("totalSpent")))
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public List<UserSpenderResponse> getTopBuyers(int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(1, Math.min(limit, 100)));
        Page<Map<String, Object>> results = orderRepository.getTopBuyers(pageable);
        
        return results.getContent().stream().map(row -> UserSpenderResponse.builder()
                .userId((String) row.get("userId"))
                .totalOrders(((Long) row.get("totalOrders")))
                .totalSpent(BigDecimal.valueOf((Double) row.get("totalSpent")))
                .build()
        ).collect(Collectors.toList());
    }

    @Override
    public UserOrderSummaryResponse getUserSummary(String userId) {
        Map<String, Object> summary = orderRepository.getUserSummary(userId);
        
        if (summary == null || summary.isEmpty() || summary.get("userId") == null) {
            return UserOrderSummaryResponse.builder()
                    .userId(userId)
                    .totalOrders(0L)
                    .totalSpent(BigDecimal.ZERO)
                    .lastOrderDate(null)
                    .build();
        }

        return UserOrderSummaryResponse.builder()
                .userId((String) summary.get("userId"))
                .totalOrders((Long) summary.get("totalOrders"))
                .totalSpent(BigDecimal.valueOf((Double) summary.get("totalSpent")))
                .lastOrderDate((LocalDateTime) summary.get("lastOrderDate"))
                .build();
    }

    @Override
    public Long countOrdersByUserId(String userId) {
        return orderRepository.countByCustomerId(userId);
    }

    @Override
    public boolean hasUserPurchasedAndReceivedBook(String userId, String bookId) {
        return orderRepository.existsByCustomerIdAndBookIdAndStatus(userId, bookId, OrderStatus.DELIVERED);
    }

    @Override
    public AdminStatsResponse getAdminAiStats() {
        Map<String, Object> stats = orderRepository.getAdminAiStats();

        Long totalOrders = (Long) stats.getOrDefault("totalOrders", 0L);
        Double totalRevenueDouble = (Double) stats.get("totalRevenue");
        BigDecimal totalRevenue = totalRevenueDouble != null ? BigDecimal.valueOf(totalRevenueDouble) : BigDecimal.ZERO;

        Long pendingOrders = (Long) stats.getOrDefault("pendingOrders", 0L);
        Long completedOrders = (Long) stats.getOrDefault("completedOrders", 0L);
        Long cancelledOrders = (Long) stats.getOrDefault("cancelledOrders", 0L);

        return AdminStatsResponse.builder()
                .totalOrders(totalOrders.intValue())
                .totalRevenue(totalRevenue)
                .pendingOrders(pendingOrders.intValue())
                .completedOrders(completedOrders.intValue())
                .cancelledOrders(cancelledOrders.intValue())
                .currency("VND")
                .build();
    }
    
    private OrderResponse mapToResponse(Order order) {
        List<OrderItemResponse> itemResponses = orderItemRepository
                .findByOrder_OrderID(order.getOrderID())
                .stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getOrderItemID())
                        .bookId(item.getBookId())
                        .quantity(item.getQuantity())
                        .unitPrice(item.getUnitPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        OrderResponse.OrderResponseBuilder builder = OrderResponse.builder()
                .id(order.getOrderID())
                .orderDate(order.getOrderDate())
                .status(order.getStatus().name())
                .total(order.getTotalAmount())
                .paymentMethod(order.getPaymentMethod())
                .taxAmount(order.getTaxAmount())
                .shippingFee(order.getShippingFee())
                .promotionId(order.getPromotionId())
                .discountAmount(order.getDiscountAmount())
                .customerId(order.getCustomerId())
                .items(itemResponses);

        ShippingDetails shippingDetails = order.getShippingDetails();
        if (shippingDetails != null) {
            builder.recipientName(shippingDetails.getRecipientName())
                .recipientPhone(shippingDetails.getPhoneNumber())
                .shippingAddress(shippingDetails.getFullAddress())
                .shippingProvince(shippingDetails.getProvince())
                .shippingDistrict(shippingDetails.getDistrict())
                .shippingWard(shippingDetails.getWard())
                .shippingNote(shippingDetails.getShippingNote());
        }

        return builder.build();
    }
}
