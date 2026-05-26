package com.notfound.orderservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AddressResponse {
    private String id;
    private String recipientName;
    private String phoneNumber;
    private String fullAddress;
    private String province;
    private String district;
    private String ward;
}