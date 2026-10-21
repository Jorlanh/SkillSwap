package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.PrivacyConsent;
import br.com.teamss.skillswap.skill_swap.model.repositories.PrivacyConsentRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class PrivacyConsentService {
    private final PrivacyConsentRepository repository;

    public PrivacyConsentService(PrivacyConsentRepository repository) {
        this.repository = repository;
    }

    public boolean isGranted(UUID userId, String purpose) {
        return repository.findTopByUser_UserIdAndPurposeOrderByRecordedAtDesc(userId, purpose)
            .map(PrivacyConsent::isGranted)
            .orElse(false);
    }
}
