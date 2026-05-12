package com.notfound.orderservice.client.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionApplyResponse {
    private boolean isValid;
    private BigDecimal discountAmount;
    private BigDecimal finalTotal;
}