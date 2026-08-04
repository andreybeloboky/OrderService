package com.beloboki.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public record PaymentCardResponse(
        Long id,
        String holder,
        String number,
        LocalDateTime expirationDate,
        Boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt)
        implements Serializable {}
