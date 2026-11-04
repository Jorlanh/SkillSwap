package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminBanDTO(Long id, UUID userId, String username, String reason, String scope,
                          Instant createdAt, Instant expiresAt, boolean active) {}
