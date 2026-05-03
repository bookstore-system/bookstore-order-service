package com.notfound.orderservice.repository;

import com.notfound.orderservice.model.entity.Order;
import com.notfound.orderservice.model.enums.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdOrderByOrderDateDesc(String customerId);

    Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);

    List<Order> findByStatus(OrderStatus status);
}
