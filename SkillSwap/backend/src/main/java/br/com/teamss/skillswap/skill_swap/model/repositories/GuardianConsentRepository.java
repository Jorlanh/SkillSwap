package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.GuardianConsent;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GuardianConsentRepository extends JpaRepository<GuardianConsent, UUID> {
    Optional<GuardianConsent> findByTokenHash(String tokenHash);
    Optional<GuardianConsent> findTopByUser_UserIdOrderByRequestedAtDesc(UUID userId);
    void deleteByUser_UserId(UUID userId);
}
