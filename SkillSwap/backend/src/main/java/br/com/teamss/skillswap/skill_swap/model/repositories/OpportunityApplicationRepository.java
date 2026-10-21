package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.OpportunityApplication;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityApplicationRepository extends JpaRepository<OpportunityApplication, UUID> {
    List<OpportunityApplication> findByApplicant_UserIdOrderByCreatedAtDesc(UUID applicantId);
    boolean existsByApplicant_UserIdAndOpportunity_OpportunityId(UUID applicantId, UUID opportunityId);
    long countByApplicant_UserId(UUID applicantId);
    List<OpportunityApplication> findByOpportunity_Organization_OrganizationIdOrderByCreatedAtDesc(UUID organizationId);
    long countByOpportunity_Organization_OrganizationId(UUID organizationId);
    long countByOpportunity_OpportunityId(UUID opportunityId);
}
