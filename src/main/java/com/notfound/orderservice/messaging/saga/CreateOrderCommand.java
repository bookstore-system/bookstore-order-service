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
public class CreateOrderCommand extends BaseSagaMessage {
    private String authorization;
    private String addressId;
    private String paymentMethod;
    private String note;
    private String discountCode;
    private String redirectUrl;
    private List<String> bookIds;
}
