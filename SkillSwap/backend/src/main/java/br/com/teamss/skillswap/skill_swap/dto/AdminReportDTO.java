package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.UUID;

public record AdminReportDTO(Long id, UUID reporterId, String reporterUsername,
                             UUID reportedUserId, String reportedUsername,
                             String reason, String status, Instant createdAt,
                             String targetType, String targetId, String category) {}
