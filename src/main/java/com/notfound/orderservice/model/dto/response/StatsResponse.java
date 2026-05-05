package com.notfound.orderservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class StatsResponse {
    BigDecimal totalRevenue;
    Long totalOrders;
    BigDecimal avgRevenuePerUser;
    BigDecimal avgOrderValue;
    String currency;
}