package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinWallet;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;

public interface SwapCoinWalletRepository extends JpaRepository<SwapCoinWallet, UUID> {
    Optional<SwapCoinWallet> findByOwner_UserId(UUID userId);

    @Query("select count(w) from SwapCoinWallet w")
    long countWallets();

    @Query("select coalesce(sum(w.balance), 0) from SwapCoinWallet w")
    long sumBalances();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select wallet from SwapCoinWallet wallet where wallet.owner.userId = :userId")
    Optional<SwapCoinWallet> findByOwnerUserIdForUpdate(@Param("userId") UUID userId);
}
