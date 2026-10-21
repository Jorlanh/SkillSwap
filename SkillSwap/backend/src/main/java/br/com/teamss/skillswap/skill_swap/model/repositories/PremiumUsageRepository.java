package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.premium.PremiumUsage;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PremiumUsageRepository extends JpaRepository<PremiumUsage, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from PremiumUsage u where u.owner.userId = :userId and u.periodStart = :periodStart")
    Optional<PremiumUsage> findForUpdate(@Param("userId") UUID userId, @Param("periodStart") LocalDate periodStart);
    Optional<PremiumUsage> findByOwner_UserIdAndPeriodStart(UUID userId, LocalDate periodStart);

    @Query("select coalesce(sum(u.freeAiPromptsUsed), 0) from PremiumUsage u")
    long sumFreeAiPromptsUsed();
}
