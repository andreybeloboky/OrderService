package com.beloboki.dto;

import com.beloboki.model.Status;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;

@ConditionalOnBean
public record OrderResponse(
        Long id,
        UserResponse user,
        Status status,
        BigDecimal totalPrice,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<OrderItemResponse> orderItems)
        implements Serializable {}
