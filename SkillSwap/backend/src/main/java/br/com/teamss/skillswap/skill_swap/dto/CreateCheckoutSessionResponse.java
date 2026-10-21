package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

public record CreateCheckoutSessionResponse(UUID paymentId, String checkoutUrl) {}
