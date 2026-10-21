package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.InstitutionalDto;
import br.com.teamss.skillswap.skill_swap.model.services.InstitutionalBillingService;
import br.com.teamss.skillswap.skill_swap.model.services.InstitutionalWorkspaceService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/institutional")
public class InstitutionalController {
    private final InstitutionalWorkspaceService workspaceService;
    private final InstitutionalBillingService billingService;

    public InstitutionalController(InstitutionalWorkspaceService workspaceService, InstitutionalBillingService billingService) {
        this.workspaceService = workspaceService;
        this.billingService = billingService;
    }

    @GetMapping("/catalog")
    public List<InstitutionalDto.PlanCatalogItem> catalog() { return InstitutionalWorkspaceService.catalog(); }
    @GetMapping("/organizations")
    public List<InstitutionalDto.OrganizationResponse> organizations() { return workspaceService.organizations(); }
    @PostMapping("/organizations")
    public InstitutionalDto.OrganizationResponse createOrganization(@Valid @RequestBody InstitutionalDto.OrganizationRequest request) { return workspaceService.createOrganization(request); }
    @GetMapping("/organizations/{organizationId}/dashboard")
    public InstitutionalDto.DashboardResponse dashboard(@PathVariable UUID organizationId) { return workspaceService.dashboard(organizationId); }
    @GetMapping("/organizations/{organizationId}/members")
    public List<InstitutionalDto.MemberResponse> members(@PathVariable UUID organizationId) { return workspaceService.members(organizationId); }
    @PostMapping("/organizations/{organizationId}/members")
    public InstitutionalDto.MemberResponse addMember(@PathVariable UUID organizationId, @Valid @RequestBody InstitutionalDto.MemberRequest request) { return workspaceService.addMember(organizationId, request); }
    @PatchMapping("/organizations/{organizationId}/members/{membershipId}")
    public InstitutionalDto.MemberResponse updateMember(@PathVariable UUID organizationId, @PathVariable UUID membershipId, @Valid @RequestBody InstitutionalDto.MemberUpdateRequest request) { return workspaceService.updateMember(organizationId, membershipId, request); }
    @DeleteMapping("/organizations/{organizationId}/members/{membershipId}")
    public ResponseEntity<Void> removeMember(@PathVariable UUID organizationId, @PathVariable UUID membershipId) { workspaceService.removeMember(organizationId, membershipId); return ResponseEntity.noContent().build(); }
    @GetMapping("/organizations/{organizationId}/opportunities")
    public List<InstitutionalDto.OpportunityResponse> opportunities(@PathVariable UUID organizationId) { return workspaceService.opportunities(organizationId); }
    @PostMapping("/organizations/{organizationId}/opportunities")
    public InstitutionalDto.OpportunityResponse createOpportunity(@PathVariable UUID organizationId, @Valid @RequestBody InstitutionalDto.OpportunityRequest request) { return workspaceService.createOpportunity(organizationId, request); }
    @PatchMapping("/organizations/{organizationId}/opportunities/{opportunityId}/status")
    public InstitutionalDto.OpportunityResponse updateOpportunityStatus(@PathVariable UUID organizationId, @PathVariable UUID opportunityId, @Valid @RequestBody InstitutionalDto.StatusRequest request) { return workspaceService.updateOpportunityStatus(organizationId, opportunityId, request.status()); }
    @GetMapping("/organizations/{organizationId}/applications")
    public List<InstitutionalDto.ApplicationResponse> applications(@PathVariable UUID organizationId) { return workspaceService.applications(organizationId); }
    @PatchMapping("/organizations/{organizationId}/applications/{applicationId}/status")
    public InstitutionalDto.ApplicationResponse updateApplicationStatus(@PathVariable UUID organizationId, @PathVariable UUID applicationId, @Valid @RequestBody InstitutionalDto.StatusRequest request) { return workspaceService.updateApplicationStatus(organizationId, applicationId, request.status()); }
    @GetMapping("/organizations/{organizationId}/subscription")
    public InstitutionalDto.SubscriptionResponse subscription(@PathVariable UUID organizationId) { return workspaceService.subscription(organizationId); }
    @PutMapping("/organizations/{organizationId}/subscription")
    public InstitutionalDto.SubscriptionResponse updateSubscription(@PathVariable UUID organizationId, @Valid @RequestBody InstitutionalDto.SubscriptionRequest request) { return workspaceService.updateSubscription(organizationId, request); }
    @PostMapping("/organizations/{organizationId}/billing/checkout")
    public InstitutionalDto.CheckoutResponse createCheckout(@PathVariable UUID organizationId, @Valid @RequestBody InstitutionalDto.SubscriptionRequest request) { return billingService.createCheckout(organizationId, request); }
}
