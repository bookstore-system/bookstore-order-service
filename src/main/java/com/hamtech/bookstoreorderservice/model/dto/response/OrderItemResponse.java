package com.hamtech.bookstoreorderservice.model.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItemResponse {
    UUID id;
    String bookId;
    String bookTitle;
    String bookImageUrl;
    Integer quantity;
    Double unitPrice;
    Double subtotal;
}
