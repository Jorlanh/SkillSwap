package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class OpportunityOsDto {
    private OpportunityOsDto() {}

    public record PlanRequest(
        String goal,
        String targetRole,
        String constraints,
        Integer weeklyHours
    ) {}

    public record StepUpdateRequest(String status) {}

    public record EvidenceRequest(
        String title,
        String skillFocus,
        String description,
        String artifactUrl,
        String evidenceType,
        UUID opportunityId,
        Long planStepId
    ) {}

    public record ApplicationRequest(String message) {}

    public record StepResponse(
        Long stepId,
        int stepOrder,
        String title,
        String description,
        String kind,
        String skillFocus,
        int estimatedHours,
        String status,
        UUID evidenceId
    ) {}

    public record PlanResponse(
        UUID planId,
        String goal,
        String targetRole,
        String constraints,
        int weeklyHours,
        String status,
        int progressPercent,
        Instant createdAt,
        Instant updatedAt,
        List<StepResponse> steps
    ) {}

    public record EvidenceResponse(
        UUID evidenceId,
        String title,
        String skillFocus,
        String description,
        String artifactUrl,
        String evidenceType,
        String verificationStatus,
        UUID opportunityId,
        Long planStepId,
        Instant createdAt,
        Instant updatedAt
    ) {}

    public record ApplicationResponse(
        UUID applicationId,
        UUID opportunityId,
        String organizationName,
        String opportunityTitle,
        String status,
        String message,
        Instant createdAt
    ) {}

    public record DashboardResponse(
        PlanResponse plan,
        long evidenceCount,
        long applicationCount,
        String nextAction
    ) {}
}
