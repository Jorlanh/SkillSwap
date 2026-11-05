package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SuspensionAppealRequest(
    @NotBlank(message = "Escreva o motivo da contestação.")
    @Size(max = 4000, message = "A contestação deve ter no máximo 4.000 caracteres.")
    String message
) {}
