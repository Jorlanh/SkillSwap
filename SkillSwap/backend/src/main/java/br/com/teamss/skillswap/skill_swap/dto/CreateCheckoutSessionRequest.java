package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateCheckoutSessionRequest(@NotBlank String packageCode) {}
