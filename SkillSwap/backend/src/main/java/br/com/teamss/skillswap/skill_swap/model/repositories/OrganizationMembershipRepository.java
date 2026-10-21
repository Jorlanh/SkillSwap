package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.OrganizationMembership;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrganizationMembershipRepository extends JpaRepository<OrganizationMembership, UUID> {
    List<OrganizationMembership> findByUser_UserIdAndStatusOrderByCreatedAtDesc(UUID userId, String status);
    List<OrganizationMembership> findByOrganization_OrganizationIdAndStatusOrderByCreatedAtAsc(UUID organizationId, String status);
    Optional<OrganizationMembership> findByOrganization_OrganizationIdAndUser_UserId(UUID organizationId, UUID userId);
    Optional<OrganizationMembership> findByMembershipIdAndOrganization_OrganizationId(UUID membershipId, UUID organizationId);
    boolean existsByOrganization_OrganizationIdAndUser_UserId(UUID organizationId, UUID userId);
    long countByOrganization_OrganizationIdAndStatus(UUID organizationId, String status);
}
