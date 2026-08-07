package com.beloboki.dto;

import com.beloboki.model.Status;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record OrderRequest(
        @NotBlank(message = "Email shouldn't be empty") @Email String userEmail,
        @NotNull Status status,
        @NotEmpty(message = "Items shouldn't be empty") @Valid List<OrderItemRequest> orderItems) {}
