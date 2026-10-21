package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record UserManagementDTO(
    UUID userId,
    String username,
    String name,
    String email,
    boolean verifiedBadge,
    Set<String> roles,
    boolean banned,
    Instant createdAt,
    long walletBalance,
    long karmaPoints
) {}
