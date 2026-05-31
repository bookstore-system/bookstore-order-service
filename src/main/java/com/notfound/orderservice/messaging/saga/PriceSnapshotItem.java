package com.notfound.orderservice.messaging.saga;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PriceSnapshotItem {
    private String bookId;
    private Integer quantity;
    private BigDecimal unitPrice;
}
