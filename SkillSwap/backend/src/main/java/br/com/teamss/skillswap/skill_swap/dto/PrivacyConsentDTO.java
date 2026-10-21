package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.UUID;

public record PrivacyConsentDTO(
    UUID consentId,
    String purpose,
    String policyVersion,
    boolean granted,
    String locale,
    String source,
    Instant recordedAt,
    Instant withdrawnAt
) {}
