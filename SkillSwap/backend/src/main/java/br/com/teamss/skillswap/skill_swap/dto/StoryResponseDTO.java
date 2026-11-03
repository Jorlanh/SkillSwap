package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.UUID;

public record StoryResponseDTO(
    Long storyId,
    UUID userId,
    String username,
    String mediaUrl,
    String mediaType,
    Instant createdAt,
    Instant expiresAt
) {}
