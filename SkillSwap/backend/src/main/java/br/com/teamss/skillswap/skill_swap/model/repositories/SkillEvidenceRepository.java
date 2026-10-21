package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.SkillEvidence;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SkillEvidenceRepository extends JpaRepository<SkillEvidence, UUID> {
    long countByOwner_UserIdIn(List<UUID> userIds);
    List<SkillEvidence> findByOwner_UserIdOrderByCreatedAtDesc(UUID ownerId);
    Optional<SkillEvidence> findByEvidenceIdAndOwner_UserId(UUID evidenceId, UUID ownerId);
    long countByOwner_UserId(UUID ownerId);
}
