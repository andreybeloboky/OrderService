package com.beloboki.kafka.event;

import com.beloboki.model.PaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentEvent(
        String eventType,
        String paymentId,
        Long orderId,
        Long userId,
        PaymentStatus status,
        BigDecimal paymentAmount,
        LocalDateTime timestamp
) {}
