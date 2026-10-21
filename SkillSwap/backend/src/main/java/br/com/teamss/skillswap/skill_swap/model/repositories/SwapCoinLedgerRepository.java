package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinLedgerEntry;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import br.com.teamss.skillswap.skill_swap.model.wallet.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwapCoinLedgerRepository extends JpaRepository<SwapCoinLedgerEntry, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    Page<SwapCoinLedgerEntry> findByWallet_Owner_UserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);

    @Query("select coalesce(sum(case when e.amount > 0 then e.amount else 0 end), 0) from SwapCoinLedgerEntry e")
    long sumCredits();

    @Query("select coalesce(sum(case when e.amount < 0 then -e.amount else 0 end), 0) from SwapCoinLedgerEntry e")
    long sumDebits();

    @Query("select coalesce(sum(case when e.amount < 0 then -e.amount else 0 end), 0) from SwapCoinLedgerEntry e where e.entryType = :type")
    long sumDebitsByType(@Param("type") LedgerEntryType type);

    @Query("select coalesce(sum(case when e.amount > 0 then e.amount else 0 end), 0) from SwapCoinLedgerEntry e where e.entryType = :type")
    long sumCreditsByType(@Param("type") LedgerEntryType type);
}
