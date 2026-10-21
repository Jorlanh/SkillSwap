package br.com.teamss.skillswap.skill_swap.model.wallet;

import java.util.Arrays;

/**
 * Server-side catalogue. Prices and coin balances are never supplied by the
 * browser, preventing a customer from altering a checkout amount.
 */
public enum SwapCoinPackage {
    STARTER("starter", "Starter", 100, 1_000, 0, false),
    STANDARD("standard", "Standard", 270, 2_500, 20, false),
    PRO("pro", "Pro", 565, 5_000, 65, false),
    ELITE("elite", "Elite", 1_200, 10_000, 200, true);

    private final String code;
    private final String displayName;
    private final long coins;
    private final long priceCents;
    private final long bonusCoins;
    private final boolean recommended;

    SwapCoinPackage(String code, String displayName, long coins, long priceCents, long bonusCoins, boolean recommended) {
        this.code = code;
        this.displayName = displayName;
        this.coins = coins;
        this.priceCents = priceCents;
        this.bonusCoins = bonusCoins;
        this.recommended = recommended;
    }

    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public long getCoins() { return coins; }
    public long getPriceCents() { return priceCents; }
    public long getBonusCoins() { return bonusCoins; }
    public boolean isRecommended() { return recommended; }

    public static SwapCoinPackage fromCode(String code) {
        return Arrays.stream(values())
            .filter(item -> item.code.equalsIgnoreCase(code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Pacote de SwapCoins inválido."));
    }
}
