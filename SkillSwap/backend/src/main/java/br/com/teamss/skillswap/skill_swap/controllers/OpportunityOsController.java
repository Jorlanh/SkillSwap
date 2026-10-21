package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.OpportunityOsDto;
import br.com.teamss.skillswap.skill_swap.model.services.OpportunityOsService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/opportunity-os")
@PreAuthorize("isAuthenticated()")
public class OpportunityOsController {
    private final OpportunityOsService service;

    public OpportunityOsController(OpportunityOsService service) { this.service = service; }

    @GetMapping("/dashboard")
    public OpportunityOsDto.DashboardResponse dashboard() { return service.dashboard(); }

    @PostMapping("/plan")
    public OpportunityOsDto.PlanResponse createPlan(@Valid @RequestBody OpportunityOsDto.PlanRequest request) {
        return service.createPlan(request);
    }

    @PatchMapping("/plan/steps/{stepId}")
    public OpportunityOsDto.PlanResponse updateStep(@PathVariable Long stepId,
                                                     @RequestBody OpportunityOsDto.StepUpdateRequest request) {
        return service.updateStep(stepId, request);
    }

    @GetMapping("/evidence")
    public List<OpportunityOsDto.EvidenceResponse> evidences() { return service.evidences(); }

    @PostMapping("/evidence")
    public OpportunityOsDto.EvidenceResponse createEvidence(@RequestBody OpportunityOsDto.EvidenceRequest request) {
        return service.createEvidence(request);
    }

    @PostMapping("/evidence/{evidenceId}/review")
    public OpportunityOsDto.EvidenceResponse requestReview(@PathVariable UUID evidenceId) {
        return service.requestEvidenceReview(evidenceId);
    }

    @GetMapping("/applications")
    public List<OpportunityOsDto.ApplicationResponse> applications() { return service.applications(); }

    @PostMapping("/opportunities/{opportunityId}/apply")
    public OpportunityOsDto.ApplicationResponse apply(@PathVariable UUID opportunityId,
                                                       @RequestBody OpportunityOsDto.ApplicationRequest request) {
        return service.apply(opportunityId, request);
    }
}
