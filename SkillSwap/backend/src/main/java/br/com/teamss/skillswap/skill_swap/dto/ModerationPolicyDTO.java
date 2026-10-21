package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ModerationPolicyDTO(
        boolean automaticTemporaryBanEnabled,
        @Min(1) @Max(168) int automaticTemporaryBanHours
) {
}
