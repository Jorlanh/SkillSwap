package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.entities.ModerationAction;
import br.com.teamss.skillswap.skill_swap.model.entities.Report;
import br.com.teamss.skillswap.skill_swap.model.entities.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ModerationCaseDTO(
        Long reportId,
        UUID reporterId,
        String reporterUsername,
        UUID reportedUserId,
        String reportedUsername,
        boolean reportedUserIsAdmin,
        String reason,
        String reportedContent,
        String source,
        ReportStatus status,
        String aiCategory,
        Double aiConfidence,
        String aiRecommendation,
        boolean contentContained,
        Instant createdAt,
        ModerationAction action,
        String decisionReason,
        String decisionEvidence,
        UUID reviewedBy,
        Instant reviewedAt,
        String appealStatus,
        String appealReason,
        Instant appealedAt,
        boolean automaticTemporaryBanApplied,
        Instant automaticActionAt
) {
    public static ModerationCaseDTO from(Report report) {
        boolean admin = report.getReportedUser() != null && report.getReportedUser().getRoles() != null
                && report.getReportedUser().getRoles().stream()
                .anyMatch(role -> "ADMIN".equalsIgnoreCase(role.getName()));
        return new ModerationCaseDTO(
                report.getId(),
                report.getReporter().getUserId(),
                report.getReporter().getUsername(),
                report.getReportedUser().getUserId(),
                report.getReportedUser().getUsername(),
                admin,
                report.getReason(),
                report.getReportedContent(),
                report.getSource(),
                report.getStatus(),
                report.getAiCategory(),
                report.getAiConfidence() == null ? null : report.getAiConfidence().doubleValue(),
                report.getAiRecommendation(),
                report.getContainedUntil() != null && report.getContainedUntil().isAfter(Instant.now()),
                report.getCreatedAt(),
                report.getAction(),
                report.getDecisionReason(),
                report.getDecisionEvidence(),
                report.getReviewedBy() == null ? null : report.getReviewedBy().getUserId(),
                report.getReviewedAt(),
                report.getAppealStatus(),
                report.getAppealReason(),
                report.getAppealedAt(),
                report.isAutomaticTemporaryBanApplied(),
                report.getAutomaticActionAt()
        );
    }
}
