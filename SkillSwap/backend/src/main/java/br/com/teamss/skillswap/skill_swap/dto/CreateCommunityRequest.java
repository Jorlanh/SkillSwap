package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCommunityRequest(
    @NotBlank @Size(max = 120) String name,
    @Size(max = 1000) String description,
    boolean privateCommunity
) {}
