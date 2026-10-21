package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinLedgerEntry;
import java.time.Instant;

public record SwapCoinLedgerResponse(Long id, String type, long amount, long balanceAfter, String description, Instant createdAt) {
    public static SwapCoinLedgerResponse from(SwapCoinLedgerEntry entry) {
        return new SwapCoinLedgerResponse(entry.getEntryId(), entry.getEntryType().name(), entry.getAmount(), entry.getBalanceAfter(), entry.getDescription(), entry.getCreatedAt());
    }
}
