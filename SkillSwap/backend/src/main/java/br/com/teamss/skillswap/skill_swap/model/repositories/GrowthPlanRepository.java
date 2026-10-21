package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.GrowthPlan;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthPlanRepository extends JpaRepository<GrowthPlan, UUID> {
    long countByOwner_UserIdIn(List<UUID> userIds);
    Optional<GrowthPlan> findFirstByOwner_UserIdAndStatusOrderByUpdatedAtDesc(UUID ownerId, String status);
    Optional<GrowthPlan> findFirstByOwner_UserIdOrderByUpdatedAtDesc(UUID ownerId);
    List<GrowthPlan> findByOwner_UserIdOrderByUpdatedAtDesc(UUID ownerId);
}
