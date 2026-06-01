package com.notfound.orderservice.model.dto.response;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BookSalesStatsResponse {
    BigDecimal totalRevenue;
    Long soldBookCount;
    BigDecimal averageRevenuePerBook;
    String currency;
}
