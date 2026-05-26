package com.notfound.orderservice.messaging.saga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class OrderCreatedEvent extends BaseSagaMessage {
    private Double totalAmount;
    private List<StockItemPayload> items;
    private String recipientName;
    private String recipientPhone;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private String shippingNote;
}
