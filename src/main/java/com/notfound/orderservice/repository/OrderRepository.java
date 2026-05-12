package com.notfound.orderservice.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.notfound.orderservice.model.entity.Order;
import com.notfound.orderservice.model.enums.OrderStatus;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {
    List<Order> findByCustomerIdOrderByOrderDateDesc(String customerId);

       long countByCustomerId(String customerId);

    Page<Order> findAllByOrderByOrderDateDesc(Pageable pageable);

    List<Order> findByStatus(OrderStatus status);

    @Query("SELECT o FROM Order o WHERE o.status = :status " +
           "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR o.orderDate <= :endDate) " +
           "ORDER BY o.orderDate DESC")
    List<Order> findByStatusAndDateRange(@Param("status") OrderStatus status,
                                         @Param("startDate") LocalDateTime startDate,
                                         @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status != 'CANCELLED' " +
           "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR o.orderDate <= :endDate)")
    Double getTotalRevenue(@Param("startDate") LocalDateTime startDate,
                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT SUM(o.totalAmount) as totalRevenue, COUNT(o) as totalOrders, " +
           "(SUM(o.totalAmount) / COUNT(DISTINCT o.customerId)) as avgRevenuePerUser, " +
           "(SUM(o.totalAmount) / COUNT(o)) as avgOrderValue " +
           "FROM Order o WHERE o.status != 'CANCELLED' " +
           "AND (:startDate IS NULL OR o.orderDate >= :startDate) " +
           "AND (:endDate IS NULL OR o.orderDate <= :endDate)")
    Map<String, Object> getGlobalStats(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT o.customerId as userId, COUNT(o) as totalOrders, SUM(o.totalAmount) as totalSpent " +
           "FROM Order o WHERE o.status != 'CANCELLED' GROUP BY o.customerId ORDER BY SUM(o.totalAmount) DESC")
    Page<Map<String, Object>> getTopSpenders(Pageable pageable);

    @Query("SELECT o.customerId as userId, COUNT(o) as totalOrders, SUM(o.totalAmount) as totalSpent " +
           "FROM Order o WHERE o.status != 'CANCELLED' GROUP BY o.customerId ORDER BY COUNT(o) DESC")
    Page<Map<String, Object>> getTopBuyers(Pageable pageable);

    @Query("SELECT o.customerId as userId, COUNT(o) as totalOrders, SUM(o.totalAmount) as totalSpent, MAX(o.orderDate) as lastOrderDate " +
           "FROM Order o WHERE o.customerId = :userId AND o.status != 'CANCELLED' GROUP BY o.customerId")
    Map<String, Object> getUserSummary(@Param("userId") String userId);

    @Query("SELECT " +
           "COUNT(o) as totalOrders, " +
           "SUM(CASE WHEN o.status != 'CANCELLED' THEN o.totalAmount ELSE 0 END) as totalRevenue, " +
           "SUM(CASE WHEN o.status = 'PENDING' THEN 1 ELSE 0 END) as pendingOrders, " +
           "SUM(CASE WHEN o.status = 'DELIVERED' THEN 1 ELSE 0 END) as completedOrders, " +
           "SUM(CASE WHEN o.status = 'CANCELLED' THEN 1 ELSE 0 END) as cancelledOrders " +
           "FROM Order o")
    Map<String, Object> getAdminAiStats();

    @Query("SELECT CASE WHEN COUNT(oi) > 0 THEN true ELSE false END FROM Order o JOIN o.orderItems oi " +
           "WHERE o.customerId = :userId AND oi.bookId = :bookId AND o.status = :status")
    boolean existsByCustomerIdAndBookIdAndStatus(@Param("userId") String userId, @Param("bookId") String bookId, @Param("status") OrderStatus status);
}
