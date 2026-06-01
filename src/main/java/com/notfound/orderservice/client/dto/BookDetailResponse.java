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
public class BookDetailResponse {
    private String bookId;
    private String title;
    private String imageUrl;
    private BigDecimal price;
    private BigDecimal salePrice;
    private Integer stockQuantity;
}
