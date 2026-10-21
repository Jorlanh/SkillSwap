package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.GrowthPlanStep;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GrowthPlanStepRepository extends JpaRepository<GrowthPlanStep, Long> {
    List<GrowthPlanStep> findByPlan_PlanIdOrderByStepOrderAsc(java.util.UUID planId);
    Optional<GrowthPlanStep> findByStepIdAndPlan_Owner_UserId(Long stepId, java.util.UUID ownerId);
}
