package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record AccessLocationRequest(
        @Min(-90) @Max(90) double latitude,
        @Min(-180) @Max(180) double longitude
) {}
