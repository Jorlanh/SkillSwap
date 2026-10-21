package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.PrivacyConsent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrivacyConsentRepository extends JpaRepository<PrivacyConsent, UUID> {
    List<PrivacyConsent> findByUser_UserIdOrderByRecordedAtDesc(UUID userId);
    Optional<PrivacyConsent> findTopByUser_UserIdAndPurposeOrderByRecordedAtDesc(UUID userId, String purpose);
}
