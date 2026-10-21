package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminRoleRequest(
    @NotBlank @Pattern(regexp = "(?i)ADMIN|USER") String role,
    boolean enabled
) {}
