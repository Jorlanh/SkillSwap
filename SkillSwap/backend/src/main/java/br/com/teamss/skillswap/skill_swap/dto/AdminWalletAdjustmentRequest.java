package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminWalletAdjustmentRequest(
    @NotNull Long amount,
    @NotBlank @Size(max = 280) String reason,
    @Size(max = 120) String idempotencyKey
) {}
