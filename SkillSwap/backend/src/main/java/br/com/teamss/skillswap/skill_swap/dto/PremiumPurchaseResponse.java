package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

public record PremiumPurchaseResponse(String feature, long amount, long balance, UUID purchaseId) {}
