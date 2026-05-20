package com.notfound.orderservice.messaging;

import com.notfound.orderservice.client.BookClient;
import com.notfound.orderservice.client.UserClient;
import com.notfound.orderservice.client.dto.AddressResponse;
import com.notfound.orderservice.client.dto.BookDetailResponse;
import com.notfound.orderservice.config.RabbitMQConfig;
import com.notfound.orderservice.messaging.saga.CreateOrderCommand;
import com.notfound.orderservice.messaging.saga.OrderCreatedEvent;
import com.notfound.orderservice.model.dto.response.ApiResponse;
import com.notfound.orderservice.model.entity.Order;
import com.notfound.orderservice.repository.OrderRepository;
import com.notfound.orderservice.repository.ProcessedMessageRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderCommandConsumerTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProcessedMessageRepository processedMessageRepository;
    @Mock private UserClient userClient;
    @Mock private BookClient bookClient;
    @Mock private OrderEventProducer orderEventProducer;
    @Mock private ObjectMapper objectMapper;

    private OrderCommandConsumer consumer;
    private UUID sagaId;
    private UUID eventId;

    @BeforeEach
    void setUp() {
        consumer = new OrderCommandConsumer(
                orderRepository,
                processedMessageRepository,
                userClient,
                bookClient,
                orderEventProducer,
                objectMapper);
        sagaId = UUID.randomUUID();
        eventId = UUID.randomUUID();
    }

    @Test
    void createCommand_createsPendingOrderWithoutReducingStock() throws Exception {
        CreateOrderCommand command = createCommand();
        Message message = commandMessage(RabbitMQConfig.ORDER_CREATE_COMMAND_KEY);
        AddressResponse address = AddressResponse.builder()
                .recipientName("Nguyen Van A")
                .phoneNumber("0123456789")
                .fullAddress("123 Nguyen Trai")
                .province("HCM")
                .district("Q1")
                .ward("P1")
                .build();
        BookDetailResponse book = BookDetailResponse.builder()
                .bookId("book-1")
                .price(new BigDecimal("100000"))
                .salePrice(new BigDecimal("90000"))
                .stockQuantity(1)
                .build();

        when(objectMapper.readValue(message.getBody(), CreateOrderCommand.class)).thenReturn(command);
        when(processedMessageRepository.existsById(eventId)).thenReturn(false);
        when(orderRepository.findBySagaId(sagaId)).thenReturn(Optional.empty());
        when(userClient.getUserAddress("user-1", "addr-1"))
                .thenReturn(ApiResponse.<AddressResponse>builder().result(address).build());
        when(bookClient.getBatchBookDetails(any()))
                .thenReturn(ApiResponse.<List<BookDetailResponse>>builder().result(List.of(book)).build());
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setOrderID(UUID.randomUUID());
            return order;
        });

        consumer.handleOrderCommand(message);

        ArgumentCaptor<OrderCreatedEvent> eventCaptor = ArgumentCaptor.forClass(OrderCreatedEvent.class);
        verify(orderEventProducer).publishSagaOrderCreated(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getSagaId()).isEqualTo(sagaId);
        assertThat(eventCaptor.getValue().getTotalAmount()).isEqualTo(90000D);
        assertThat(eventCaptor.getValue().getItems()).hasSize(1);
        verify(bookClient, never()).reduceStock(any());
        verify(bookClient, never()).restoreStock(any());
    }

    @Test
    void duplicateEventId_skipsCreateCommand() throws Exception {
        CreateOrderCommand command = createCommand();
        Message message = commandMessage(RabbitMQConfig.ORDER_CREATE_COMMAND_KEY);

        when(objectMapper.readValue(message.getBody(), CreateOrderCommand.class)).thenReturn(command);
        when(processedMessageRepository.existsById(eventId)).thenReturn(true);

        consumer.handleOrderCommand(message);

        verify(orderRepository, never()).save(any());
        verify(orderEventProducer, never()).publishSagaOrderCreated(any());
    }

    private CreateOrderCommand createCommand() {
        return CreateOrderCommand.builder()
                .eventId(eventId)
                .sagaId(sagaId)
                .correlationId(sagaId)
                .type(RabbitMQConfig.ORDER_CREATE_COMMAND_KEY)
                .userId("user-1")
                .addressId("addr-1")
                .paymentMethod("VNPAY")
                .bookIds(List.of("book-1"))
                .build();
    }

    private Message commandMessage(String routingKey) {
        return MessageBuilder.withBody(new byte[] { 1 })
                .setReceivedRoutingKey(routingKey)
                .build();
    }
}
