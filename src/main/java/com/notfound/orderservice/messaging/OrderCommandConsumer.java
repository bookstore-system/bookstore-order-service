package com.notfound.orderservice.messaging;

import com.notfound.orderservice.client.BookClient;
import com.notfound.orderservice.client.UserClient;
import com.notfound.orderservice.client.dto.AddressResponse;
import com.notfound.orderservice.client.dto.BookBatchRequest;
import com.notfound.orderservice.client.dto.BookDetailResponse;
import com.notfound.orderservice.config.RabbitMQConfig;
import com.notfound.orderservice.exception.BusinessException;
import com.notfound.orderservice.messaging.saga.BaseSagaMessage;
import com.notfound.orderservice.messaging.saga.ConfirmOrderCommand;
import com.notfound.orderservice.messaging.saga.CreateOrderCommand;
import com.notfound.orderservice.messaging.saga.OrderCreatedEvent;
import com.notfound.orderservice.messaging.saga.SagaFailureEvent;
import com.notfound.orderservice.messaging.saga.StockItemPayload;
import com.notfound.orderservice.model.entity.Order;
import com.notfound.orderservice.model.entity.OrderItem;
import com.notfound.orderservice.model.entity.ProcessedMessage;
import com.notfound.orderservice.model.entity.ShippingDetails;
import com.notfound.orderservice.model.enums.OrderStatus;
import com.notfound.orderservice.repository.OrderRepository;
import com.notfound.orderservice.repository.ProcessedMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCommandConsumer {

    private final OrderRepository orderRepository;
    private final ProcessedMessageRepository processedMessageRepository;
    private final UserClient userClient;
    private final BookClient bookClient;
    private final OrderEventProducer orderEventProducer;
    private final ObjectMapper objectMapper;

    @RabbitListener(queues = RabbitMQConfig.ORDER_COMMANDS_QUEUE)
    @Transactional
    public void handleOrderCommand(Message message) {
        String routingKey = message.getMessageProperties().getReceivedRoutingKey();
        try {
            if (RabbitMQConfig.ORDER_CREATE_COMMAND_KEY.equals(routingKey)) {
                handleCreate(read(message, CreateOrderCommand.class));
            } else if (RabbitMQConfig.ORDER_CONFIRM_COMMAND_KEY.equals(routingKey)) {
                handleConfirm(read(message, ConfirmOrderCommand.class));
            } else if (RabbitMQConfig.ORDER_CANCEL_COMMAND_KEY.equals(routingKey)) {
                handleCancel(read(message, BaseSagaMessage.class));
            } else {
                log.warn("Ignore unsupported order saga command routingKey={}", routingKey);
            }
        } catch (Exception e) {
            log.error("Unable to deserialize order saga command routingKey={}: {}", routingKey, e.getMessage());
        }
    }

    private <T> T read(Message message, Class<T> targetType) throws Exception {
        return objectMapper.readValue(message.getBody(), targetType);
    }

    private void handleCreate(CreateOrderCommand command) {
        try {
            if (!markProcessed(command)) {
                return;
            }
            Order order = orderRepository.findBySagaId(command.getSagaId())
                    .orElseGet(() -> createPendingOrder(command));
            orderEventProducer.publishSagaOrderCreated(buildOrderCreatedEvent(command, order));
        } catch (Exception e) {
            publishFailed(command, command.getOrderId(), e);
        }
    }

    private void handleConfirm(ConfirmOrderCommand command) {
        try {
            if (!markProcessed(command)) {
                return;
            }
            Order order = findSagaOrder(command);
            if (order.getStatus() != OrderStatus.CONFIRMED) {
                if (command.getTotalAmount() != null) {
                    order.setTotalAmount(command.getTotalAmount());
                }
                if (command.getDiscountAmount() != null) {
                    order.setDiscountAmount(command.getDiscountAmount());
                }
                order.setStatus(OrderStatus.CONFIRMED);
                order = orderRepository.save(order);
            }
            orderEventProducer.publishSagaOrderConfirmed(buildSimpleEvent(command, RabbitMQConfig.ORDER_CONFIRMED_KEY, order));
        } catch (Exception e) {
            publishFailed(command, command.getOrderId(), e);
        }
    }

    private void handleCancel(BaseSagaMessage command) {
        try {
            if (!markProcessed(command)) {
                return;
            }
            Order order = findSagaOrder(command);
            if (order.getStatus() != OrderStatus.CANCELLED) {
                order.setStatus(OrderStatus.CANCELLED);
                order = orderRepository.save(order);
            }
            orderEventProducer.publishSagaOrderCancelled(buildSimpleEvent(command, RabbitMQConfig.ORDER_CANCELLED_KEY, order));
        } catch (Exception e) {
            publishFailed(command, command.getOrderId(), e);
        }
    }

    private Order createPendingOrder(CreateOrderCommand command) {
        validateCreateCommand(command);

        AddressResponse address = userClient.getUserAddress(command.getUserId(), command.getAddressId()).getResult();
        if (address == null) {
            throw new BusinessException("Khong tim thay dia chi giao hang");
        }

        ShippingDetails shippingDetails = ShippingDetails.builder()
                .recipientName(address.getRecipientName())
                .phoneNumber(address.getPhoneNumber())
                .fullAddress(address.getFullAddress())
                .province(address.getProvince())
                .district(address.getDistrict())
                .ward(address.getWard())
                .shippingNote(command.getNote())
                .build();

        Map<String, Long> bookQuantities = command.getBookIds().stream()
                .filter(bookId -> bookId != null && !bookId.isBlank())
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
        if (bookQuantities.isEmpty()) {
            throw new BusinessException("Danh sach sach can dat khong hop le");
        }

        List<String> uniqueBookIds = new ArrayList<>(bookQuantities.keySet());
        List<BookDetailResponse> bookDetails = bookClient.getBatchBookDetails(
                BookBatchRequest.builder().bookIds(uniqueBookIds).build()).getResult();
        if (bookDetails == null || bookDetails.isEmpty()) {
            throw new BusinessException("Khong lay duoc thong tin sach tu he thong");
        }

        Map<String, BookDetailResponse> detailMap = bookDetails.stream()
                .filter(detail -> detail.getBookId() != null)
                .collect(Collectors.toMap(BookDetailResponse::getBookId, Function.identity(), (a, b) -> a));

        Order order = Order.builder()
                .sagaId(command.getSagaId())
                .customerId(command.getUserId())
                .status(OrderStatus.PENDING)
                .paymentMethod(command.getPaymentMethod())
                .promotionId(command.getDiscountCode())
                .discountAmount(0D)
                .taxAmount(0D)
                .shippingDetails(shippingDetails)
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        for (Map.Entry<String, Long> entry : bookQuantities.entrySet()) {
            BookDetailResponse detail = detailMap.get(entry.getKey());
            if (detail == null) {
                throw new BusinessException("Khong tim thay thong tin sach: " + entry.getKey());
            }
            BigDecimal unitPrice = detail.getSalePrice() != null ? detail.getSalePrice() : detail.getPrice();
            if (unitPrice == null) {
                throw new BusinessException("Khong tim thay gia cho sach: " + entry.getKey());
            }
            int quantity = Math.toIntExact(entry.getValue());
            subtotal = subtotal.add(unitPrice.multiply(BigDecimal.valueOf(quantity)));
            orderItems.add(new OrderItem(order, entry.getKey(), quantity, unitPrice.doubleValue()));
        }

        order.setTotalAmount(subtotal.doubleValue());
        order.setOrderItems(orderItems);
        return orderRepository.save(order);
    }

    private void validateCreateCommand(CreateOrderCommand command) {
        if (command.getEventId() == null || command.getSagaId() == null) {
            throw new BusinessException("eventId and sagaId are required");
        }
        if (command.getUserId() == null || command.getUserId().isBlank()) {
            throw new BusinessException("userId is required");
        }
        if (command.getAddressId() == null || command.getAddressId().isBlank()) {
            throw new BusinessException("addressId is required");
        }
        if (command.getPaymentMethod() == null || command.getPaymentMethod().isBlank()) {
            throw new BusinessException("paymentMethod is required");
        }
        if (command.getBookIds() == null || command.getBookIds().isEmpty()) {
            throw new BusinessException("bookIds is required");
        }
    }

    private OrderCreatedEvent buildOrderCreatedEvent(CreateOrderCommand command, Order order) {
        ShippingDetails shipping = order.getShippingDetails();
        return OrderCreatedEvent.builder()
                .eventId(UUID.randomUUID())
                .sagaId(command.getSagaId())
                .correlationId(command.getCorrelationId())
                .causationId(command.getEventId())
                .type(RabbitMQConfig.ORDER_CREATED_KEY)
                .occurredAt(LocalDateTime.now())
                .orderId(order.getOrderID())
                .userId(order.getCustomerId())
                .totalAmount(order.getTotalAmount())
                .items(order.getOrderItems().stream()
                        .map(item -> StockItemPayload.builder()
                                .bookId(item.getBookId())
                                .quantity(item.getQuantity())
                                .build())
                        .toList())
                .recipientName(shipping != null ? shipping.getRecipientName() : null)
                .recipientPhone(shipping != null ? shipping.getPhoneNumber() : null)
                .shippingAddress(shipping != null ? shipping.getFullAddress() : null)
                .shippingProvince(shipping != null ? shipping.getProvince() : null)
                .shippingDistrict(shipping != null ? shipping.getDistrict() : null)
                .shippingWard(shipping != null ? shipping.getWard() : null)
                .shippingNote(shipping != null ? shipping.getShippingNote() : null)
                .build();
    }

    private BaseSagaMessage buildSimpleEvent(BaseSagaMessage command, String type, Order order) {
        return BaseSagaMessage.builder()
                .eventId(UUID.randomUUID())
                .sagaId(command.getSagaId())
                .correlationId(command.getCorrelationId())
                .causationId(command.getEventId())
                .type(type)
                .occurredAt(LocalDateTime.now())
                .orderId(order.getOrderID())
                .userId(order.getCustomerId())
                .build();
    }

    private Order findSagaOrder(BaseSagaMessage command) {
        if (command.getSagaId() != null) {
            return orderRepository.findBySagaId(command.getSagaId())
                    .orElseThrow(() -> new BusinessException("Khong tim thay order theo sagaId"));
        }
        if (command.getOrderId() != null) {
            return orderRepository.findById(command.getOrderId())
                    .orElseThrow(() -> new BusinessException("Khong tim thay order theo orderId"));
        }
        throw new BusinessException("sagaId or orderId is required");
    }

    private boolean markProcessed(BaseSagaMessage command) {
        if (command.getEventId() == null || command.getSagaId() == null) {
            throw new BusinessException("eventId and sagaId are required");
        }
        if (processedMessageRepository.existsById(command.getEventId())) {
            log.info("Skip duplicate order saga command eventId={} type={}", command.getEventId(), command.getType());
            return false;
        }
        processedMessageRepository.save(ProcessedMessage.builder()
                .eventId(command.getEventId())
                .sagaId(command.getSagaId())
                .messageType(command.getType())
                .processedAt(LocalDateTime.now())
                .build());
        return true;
    }

    private void publishFailed(BaseSagaMessage command, UUID orderId, Exception e) {
        log.error("Order saga command failed sagaId={}, type={}: {}", command.getSagaId(), command.getType(), e.getMessage());
        orderEventProducer.publishSagaOrderFailed(SagaFailureEvent.builder()
                .eventId(UUID.randomUUID())
                .sagaId(command.getSagaId())
                .correlationId(command.getCorrelationId())
                .causationId(command.getEventId())
                .type(RabbitMQConfig.ORDER_FAILED_KEY)
                .occurredAt(LocalDateTime.now())
                .orderId(orderId)
                .userId(command.getUserId())
                .reason(e.getMessage())
                .build());
    }
}
