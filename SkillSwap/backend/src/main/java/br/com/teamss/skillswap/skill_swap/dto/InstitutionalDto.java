package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class InstitutionalDto {
    private InstitutionalDto() {}

    public record OrganizationRequest(
        @NotBlank @Size(max = 180) String name,
        @NotBlank @Pattern(regexp = "(?i)UNIVERSITY|COMPANY|GOVERNMENT|NGO|TRAINING_PROVIDER|OTHER") String type,
        @Size(max = 220) String legalName,
        @Size(max = 80) String country,
        @Size(max = 255) String website,
        @Email @Size(max = 255) String billingEmail
    ) {}

    public record OrganizationResponse(
        UUID organizationId, String slug, String name, String legalName, String type,
        String country, String website, String billingEmail, String status,
        String planCode, String subscriptionStatus, int seatLimit, long seatUsage,
        String billingCurrency, long monthlyPriceCents, String membershipRole
    ) {}

    public record MemberRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @Pattern(regexp = "(?i)ADMIN|MANAGER|ANALYST|MEMBER") String role,
        @Size(max = 120) String department
    ) {}

    public record MemberUpdateRequest(
        @Pattern(regexp = "(?i)OWNER|ADMIN|MANAGER|ANALYST|MEMBER") String role,
        @Pattern(regexp = "(?i)ACTIVE|SUSPENDED") String status,
        @Size(max = 120) String department
    ) {}

    public record MemberResponse(
        UUID membershipId, UUID userId, String name, String username, String email,
        String role, String status, String department, Instant createdAt
    ) {}

    public record OpportunityRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 5000) String description,
        @NotBlank @Size(max = 1000) String requiredSkills,
        boolean remote,
        boolean accessibleSupport,
        @Size(max = 120) String location,
        @Pattern(regexp = "(?i)DRAFT|OPEN|CLOSED") String status
    ) {}

    public record OpportunityResponse(
        UUID opportunityId, UUID organizationId, String organizationName, String title,
        String description, String requiredSkills, boolean remote, boolean accessibleSupport,
        String location, String status, Instant createdAt, long applicationCount
    ) {}

    public record StatusRequest(@NotBlank @Pattern(regexp = "(?i)DRAFT|OPEN|CLOSED|ARCHIVED|SUBMITTED|REVIEWING|ACCEPTED|REJECTED|WITHDRAWN") String status) {}

    public record ApplicationResponse(
        UUID applicationId, UUID opportunityId, String opportunityTitle, UUID applicantId,
        String applicantName, String applicantUsername, String applicantEmail,
        String status, String message, Instant createdAt, Instant updatedAt
    ) {}

    public record DashboardResponse(
        OrganizationResponse organization, long members, long activeMembers,
        long opportunities, long openOpportunities, long applications,
        long activePlans, long evidences, Map<String, Long> applicationsByStatus,
        List<OpportunityResponse> recentOpportunities
    ) {}

    public record SubscriptionRequest(
        @NotBlank @Pattern(regexp = "(?i)TRIAL|TEAM|GROWTH|ENTERPRISE") String planCode,
        @Min(1) @Max(100000) Integer seatLimit,
        @Pattern(regexp = "(?i)BRL|EUR|USD") String currency,
        @Email @Size(max = 255) String billingEmail
    ) {}

    public record SubscriptionResponse(
        String planCode, String subscriptionStatus, int seatLimit,
        String billingCurrency, long monthlyPriceCents, String billingEmail,
        boolean stripeConfigured, String stripeCheckoutSessionId
    ) {}

    public record PlanCatalogItem(String code, String name, int defaultSeats, long monthlyUsdCents, long monthlyEurCents, long monthlyBrlCents, String description) {}

    public record CheckoutResponse(String organizationId, String planCode, String currency, String checkoutUrl, String sessionId) {}
}
