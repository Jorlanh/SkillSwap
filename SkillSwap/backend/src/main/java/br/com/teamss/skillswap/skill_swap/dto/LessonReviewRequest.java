package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LessonReviewRequest(
    @Min(1) @Max(5) Integer stars,
    @Size(max = 1200) String comment,
    @NotBlank @Size(max = 180) String skillSubject,
    @NotBlank @Size(max = 180) String fullName
) {}
