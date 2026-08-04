package com.beloboki.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ItemRequest(@NotNull String name, @NotNull BigDecimal price) {}
