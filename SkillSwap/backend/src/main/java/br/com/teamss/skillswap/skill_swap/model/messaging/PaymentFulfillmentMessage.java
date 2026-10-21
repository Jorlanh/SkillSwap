package br.com.teamss.skillswap.skill_swap.model.messaging;

public record PaymentFulfillmentMessage(String sessionId, Long amountTotal, String currency, String paymentIntent) {}
