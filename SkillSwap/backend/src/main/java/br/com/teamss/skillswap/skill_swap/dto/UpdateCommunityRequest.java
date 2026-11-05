package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Size;

public record UpdateCommunityRequest(
        @Size(max = 120) String name,
        @Size(max = 1000) String description,
        Boolean privateCommunity
) {}
