package com.hamtech.bookstoreorderservice.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CheckoutRequest {
    @NotNull
    String addressId;

    @NotBlank
    String paymentMethod;

    String note;
    String discountCode;

    List<String> bookIds;
}
