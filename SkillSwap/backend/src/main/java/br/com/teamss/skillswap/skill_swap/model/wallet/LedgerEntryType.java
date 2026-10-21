package br.com.teamss.skillswap.skill_swap.model.wallet;

/** Immutable categories used by the auditable SwapCoins ledger. */
public enum LedgerEntryType {
    PURCHASE,
    WELCOME_BONUS,
    TIP_SENT,
    TIP_RECEIVED,
    FEATURE_SPEND,
    ACHIEVEMENT_REWARD,
    LESSON_PAYMENT,
    REFUND,
    ADJUSTMENT
}
