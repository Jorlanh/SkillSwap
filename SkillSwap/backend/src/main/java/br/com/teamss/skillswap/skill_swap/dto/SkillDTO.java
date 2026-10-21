package br.com.teamss.skillswap.skill_swap.dto;

public record SkillDTO(
    Long skillId,
    String name,
    String description,
    String category,
    String level
) {}