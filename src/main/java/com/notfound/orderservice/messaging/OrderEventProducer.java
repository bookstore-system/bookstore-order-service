package com.notfound.orderservice.messaging;

import com.notfound.orderservice.config.RabbitMQConfig;
import com.notfound.orderservice.messaging.saga.BaseSagaMessage;
import com.notfound.orderservice.messaging.saga.OrderCreatedEvent;
import com.notfound.orderservice.messaging.saga.SagaFailureEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventProducer {

    private final RabbitTemplate rabbitTemplate;

    public void publishOrderCancelled(OrderPlacedEvent event) {
        log.info("Gui order.cancelled event: orderId={}", event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.ORDER_EXCHANGE, RabbitMQConfig.ORDER_CANCELLED_KEY, event);
    }

    public void publishSagaOrderCreated(OrderCreatedEvent event) {
        log.info("Publish saga order.created: sagaId={}, orderId={}", event.getSagaId(), event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENT_EXCHANGE, RabbitMQConfig.ORDER_CREATED_KEY, event);
    }

    public void publishSagaOrderConfirmed(BaseSagaMessage event) {
        log.info("Publish saga order.confirmed: sagaId={}, orderId={}", event.getSagaId(), event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENT_EXCHANGE, RabbitMQConfig.ORDER_CONFIRMED_KEY, event);
    }

    public void publishSagaOrderCancelled(BaseSagaMessage event) {
        log.info("Publish saga order.cancelled: sagaId={}, orderId={}", event.getSagaId(), event.getOrderId());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENT_EXCHANGE, RabbitMQConfig.ORDER_CANCELLED_KEY, event);
    }

    public void publishSagaOrderFailed(SagaFailureEvent event) {
        log.warn("Publish saga order.failed: sagaId={}, orderId={}, reason={}",
                event.getSagaId(), event.getOrderId(), event.getReason());
        rabbitTemplate.convertAndSend(RabbitMQConfig.EVENT_EXCHANGE, RabbitMQConfig.ORDER_FAILED_KEY, event);
    }
}
