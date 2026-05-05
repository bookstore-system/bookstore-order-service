package com.notfound.orderservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class UserOrderSummaryResponse {
    String userId;
    Long totalOrders;
    BigDecimal totalSpent;
    LocalDateTime lastOrderDate;
}