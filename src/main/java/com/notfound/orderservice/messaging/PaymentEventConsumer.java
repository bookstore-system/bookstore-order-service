package com.notfound.orderservice.messaging;

import com.notfound.orderservice.config.RabbitMQConfig;
import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final OrderService orderService;

    @RabbitListener(queues = RabbitMQConfig.PAYMENT_COMPLETED_QUEUE)
    public void handlePaymentCompleted(PaymentCompletedEvent event) {
        log.info("Nhận payment.completed event: orderId={}, status={}", event.getOrderId(), event.getStatus());
        try {
            if ("COMPLETED".equals(event.getStatus())) {
                orderService.updateOrderStatusByPayment(event.getOrderId(), OrderStatus.CONFIRMED);
                log.info("Cập nhật order {} → CONFIRMED", event.getOrderId());
            } else if ("FAILED".equals(event.getStatus())) {
                orderService.updateOrderStatusByPayment(event.getOrderId(), OrderStatus.CANCELLED);
                log.warn("Cập nhật order {} → CANCELLED (payment failed)", event.getOrderId());
            }
        } catch (Exception e) {
            log.error("Lỗi xử lý payment.completed event orderId={}: {}", event.getOrderId(), e.getMessage());
        }
    }
}
