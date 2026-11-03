package br.com.teamss.skillswap.skill_swap.dto;

import java.util.Set;
import java.util.UUID;
import java.time.Instant;

// DTO seguro para o perfil PÚBLICO. Não inclui senha, tokens ou segredos.
public record UserPublicProfileDTO(
    UUID userId,
    String username,
    String name,
    Instant createdAt,
    boolean verifiedBadge, // NOVO CAMPO
    ProfileDTO profile,
    Set<SkillDTO> skills,
    String accessibilitySymbol
) {}
