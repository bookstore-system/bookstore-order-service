package com.notfound.orderservice.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ShippingDetails {

    @Column(name = "ship_recipient_name")
    String recipientName;

    @Column(name = "ship_phone_number")
    String phoneNumber;

    @Column(name = "ship_full_address")
    String fullAddress;

    @Column(name = "ship_province")
    String province; 

    @Column(name = "ship_district")
    String district; 

    @Column(name = "ship_ward")
    String ward; 

    @Column(name = "shipping_note", columnDefinition = "TEXT")
    String shippingNote; 
}
