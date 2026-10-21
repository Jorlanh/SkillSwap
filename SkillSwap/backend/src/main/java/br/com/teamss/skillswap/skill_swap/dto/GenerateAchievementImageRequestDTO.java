package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GenerateAchievementImageRequestDTO {
    @NotNull
    private Long achievementId;

    @NotBlank
    @Size(min = 10, max = 300)
    private String userPrompt;
}