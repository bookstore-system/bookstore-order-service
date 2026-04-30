package com.hamtech.bookstoreorderservice.model.entity;

import com.hamtech.bookstoreorderservice.model.enums.OrderStatus;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = { "orderItems" })
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Order {

    @Id
    @UuidGenerator
    UUID orderID;

    @CreationTimestamp
    @Column(name = "order_date", nullable = false)
    LocalDateTime orderDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    OrderStatus status;

    @Column(name = "total_amount", nullable = false)
    Double totalAmount;

    @Column(name = "payment_method")
    String paymentMethod;

    @Column(name = "tax_amount")
    Double taxAmount;

    // Refactored: Store userId instead of ManyToOne User Entity
    @Column(name = "customer_id", nullable = false)
    String customerId;

    // Store Promotion ID instead of mapping ManyToOne Promotion Entity
    @Column(name = "promotion_id")
    String promotionId;

    @Column(name = "discount_amount")
    Double discountAmount;

    @Column(name = "shipping_fee")
    Double shippingFee;

    @Embedded
    ShippingDetails shippingDetails;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    List<OrderItem> orderItems;
}
