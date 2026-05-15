package com.notfound.orderservice.messaging;

import com.notfound.orderservice.config.RabbitMQConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderEventProducerTest {

    @Mock private RabbitTemplate rabbitTemplate;

    @InjectMocks private OrderEventProducer producer;

    private static final UUID ORDER_ID = UUID.randomUUID();

    private OrderPlacedEvent buildEvent() {
        return OrderPlacedEvent.builder()
                .orderId(ORDER_ID)
                .userId("user-123")
                .totalAmount(90000.0)
                .paymentMethod("COD")
                .createdAt(LocalDateTime.now())
                .build();
    }

    @Test
    void publishOrderPlaced_sendsToCorrectExchangeAndRoutingKey() {
        OrderPlacedEvent event = buildEvent();

        producer.publishOrderPlaced(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_PLACED_KEY,
                event
        );
    }

    @Test
    void publishOrderCancelled_sendsToCorrectExchangeAndRoutingKey() {
        OrderPlacedEvent event = buildEvent();

        producer.publishOrderCancelled(event);

        verify(rabbitTemplate).convertAndSend(
                RabbitMQConfig.ORDER_EXCHANGE,
                RabbitMQConfig.ORDER_CANCELLED_KEY,
                event
        );
    }
}
