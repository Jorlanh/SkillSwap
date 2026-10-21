package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModerationAppealRequest(
        @NotBlank @Size(max = 2000) String reason
) {
}
