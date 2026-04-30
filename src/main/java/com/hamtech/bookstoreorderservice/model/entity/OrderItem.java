package com.hamtech.bookstoreorderservice.model.entity;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.UuidGenerator;

import java.util.UUID;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"order"})
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderItem {

    @Id
    @UuidGenerator
    UUID orderItemID;

    @Column(nullable = false)
    Integer quantity;

    @Column(name = "unit_price", nullable = false)
    Double unitPrice;

    @Column(nullable = false)
    Double subtotal;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    @JsonBackReference
    Order order;

    // Refactored: Store book ID instead of ManyToOne Book Entity
    @Column(name = "book_id", nullable = false)
    String bookId;

    public OrderItem(Order order, String bookId, Integer quantity, Double unitPrice) {
        this.order = order;
        this.bookId = bookId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = quantity * unitPrice;
    }
}
