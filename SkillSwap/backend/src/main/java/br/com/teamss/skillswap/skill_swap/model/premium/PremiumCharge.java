package br.com.teamss.skillswap.skill_swap.model.premium;

import java.util.UUID;

public record PremiumCharge(UUID userId, String featureCode, long amount, boolean charged, String idempotencyKey) {
    public static PremiumCharge free(UUID userId, String featureCode) {
        return new PremiumCharge(userId, featureCode, 0, false, null);
    }
}
