package com.notfound.orderservice.messaging;

import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PaymentEventConsumerTest {

    @Mock private OrderService orderService;

    @InjectMocks private PaymentEventConsumer consumer;

    private static final UUID ORDER_ID = UUID.randomUUID();

    @Test
    void handlePaymentCompleted_statusCompleted_updatesOrderToConfirmed() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(ORDER_ID)
                .status("COMPLETED")
                .build();

        consumer.handlePaymentCompleted(event);

        verify(orderService).updateOrderStatusByPayment(ORDER_ID, OrderStatus.CONFIRMED);
    }

    @Test
    void handlePaymentCompleted_statusFailed_updatesOrderToCancelled() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(ORDER_ID)
                .status("FAILED")
                .build();

        consumer.handlePaymentCompleted(event);

        verify(orderService).updateOrderStatusByPayment(ORDER_ID, OrderStatus.CANCELLED);
    }

    @Test
    void handlePaymentCompleted_unknownStatus_doesNotUpdateOrder() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(ORDER_ID)
                .status("REFUNDED")
                .build();

        consumer.handlePaymentCompleted(event);

        verify(orderService, never()).updateOrderStatusByPayment(any(), any());
    }

    @Test
    void handlePaymentCompleted_serviceThrows_doesNotPropagateException() {
        PaymentCompletedEvent event = PaymentCompletedEvent.builder()
                .orderId(ORDER_ID)
                .status("COMPLETED")
                .build();

        doThrow(new RuntimeException("DB error"))
                .when(orderService).updateOrderStatusByPayment(eq(ORDER_ID), eq(OrderStatus.CONFIRMED));

        // exception must be caught internally — consumer must not rethrow
        org.assertj.core.api.Assertions.assertThatCode(() -> consumer.handlePaymentCompleted(event))
                .doesNotThrowAnyException();
    }
}
