package com.beloboki.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record ItemRequest(@NotNull String name, @NotNull @Positive BigDecimal price) {}
