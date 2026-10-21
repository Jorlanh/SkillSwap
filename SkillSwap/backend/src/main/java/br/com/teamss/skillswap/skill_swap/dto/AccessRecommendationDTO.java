package br.com.teamss.skillswap.skill_swap.dto;

import java.util.UUID;

public record AccessRecommendationDTO(
    UUID opportunityId,
    String organizationName,
    String title,
    String description,
    boolean remote,
    boolean accessibleSupport,
    int score,
    String explanation
) {}
