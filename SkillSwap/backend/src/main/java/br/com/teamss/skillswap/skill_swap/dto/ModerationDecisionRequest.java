package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.entities.ModerationAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ModerationDecisionRequest(
        @NotNull ModerationAction action,
        @NotBlank @Size(max = 2000) String reason,
        @Size(max = 10000) String evidence,
        Integer durationDays,
        boolean humanConfirmation
) {
}
