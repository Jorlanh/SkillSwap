package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record SwapCoinTransferRequest(
    @NotNull UUID recipientUserId,
    @Min(1) long amount,
    @Size(max = 160) String note
) {}
