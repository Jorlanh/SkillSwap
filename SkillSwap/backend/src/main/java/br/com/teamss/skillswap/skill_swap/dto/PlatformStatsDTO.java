package br.com.teamss.skillswap.skill_swap.dto;

public record PlatformStatsDTO(
    long totalUsers,
    long totalSkills,
    long totalProposals,
    long pendingProposals,
    long completedProposals
) {}