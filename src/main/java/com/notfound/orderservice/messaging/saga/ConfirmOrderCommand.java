package com.notfound.orderservice.messaging.saga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfirmOrderCommand extends BaseSagaMessage {
    private Double totalAmount;
    private Double discountAmount;
    private UUID paymentId;
    private String shippingOrderCode;
}
