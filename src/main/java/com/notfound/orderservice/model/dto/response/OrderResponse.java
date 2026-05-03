package com.notfound.orderservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderResponse {
    UUID id;
    String orderCode;
    LocalDateTime orderDate;
    String status;
    Double total;
    String paymentMethod;
    Double taxAmount;
    Double shippingFee;

    // Promotion
    String promotionId;
    Double discountAmount;

    // Customer info (will be simplified since we don't have direct access to User entity)
    String customerId;
    String customerName; // Optional, might need to fetch from user service

    // Shipping info
    String recipientName;
    String recipientPhone;
    String shippingAddress;      
    String shippingProvince;     
    String shippingDistrict;     
    String shippingWard;         
    String shippingNote;         

    List<OrderItemResponse> items;

    String note;
}
