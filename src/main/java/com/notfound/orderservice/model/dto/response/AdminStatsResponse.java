package com.notfound.orderservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AdminStatsResponse {
    Integer totalOrders;
    BigDecimal totalRevenue;
    Integer pendingOrders;
    Integer completedOrders;
    Integer cancelledOrders;
    String currency;
}