package com.notfound.orderservice.messaging.saga;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class BaseSagaMessage {
    private UUID eventId;
    private UUID sagaId;
    private UUID correlationId;
    private UUID causationId;
    private String type;
    private LocalDateTime occurredAt;
    private UUID orderId;
    private String userId;
}
