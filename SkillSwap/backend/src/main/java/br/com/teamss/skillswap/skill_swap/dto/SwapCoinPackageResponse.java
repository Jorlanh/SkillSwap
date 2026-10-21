package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinPackage;

public record SwapCoinPackageResponse(
    String code,
    String name,
    long coins,
    long priceCents,
    long bonusCoins,
    boolean recommended
) {
    public static SwapCoinPackageResponse from(SwapCoinPackage item) {
        return new SwapCoinPackageResponse(item.getCode(), item.getDisplayName(), item.getCoins(), item.getPriceCents(), item.getBonusCoins(), item.isRecommended());
    }
}
