package br.com.teamss.skillswap.skill_swap.dto;

public record AdminOperationsReportDTO(
    long totalWallets,
    long totalBalance,
    long paidPayments,
    long pendingPayments,
    long failedPayments,
    long totalRevenueCents,
    long totalPurchasedCoins,
    long totalLedgerEntries,
    long totalCredits,
    long totalDebits,
    long featureSpendCoins,
    long tipVolumeCoins,
    long lessonPaymentCoins,
    long achievementRewardCoins,
    long premiumAiPrompts,
    long extraLessonPurchases,
    long paidLessonCoins,
    long totalKarmaPoints,
    long activeBoostedProposals,
    long activeHighlightedPosts,
    long portfolioSlots,
    boolean rabbitMqEnabled
) {}
