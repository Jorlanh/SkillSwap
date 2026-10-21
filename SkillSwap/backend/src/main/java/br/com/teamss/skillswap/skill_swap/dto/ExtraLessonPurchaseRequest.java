package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record ExtraLessonPurchaseRequest(@Min(1) @Max(10) int hours) {}
