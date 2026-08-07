package com.beloboki.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "Item id shouldn't be null") Long itemId,
        @NotNull(message = "Quantity shouldn't be null")
        @Min(value = 1, message = "Quantity should be at least 1")
        Integer quantity) {
}
