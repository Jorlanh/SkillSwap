package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.AccessOpportunity;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessOpportunityRepository extends JpaRepository<AccessOpportunity, UUID> {
    List<AccessOpportunity> findTop30ByOrderByCreatedAtDesc();
}
