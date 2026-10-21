package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Ban;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface BanRepository extends JpaRepository<Ban, Long> {
    List<Ban> findByUser_UserIdAndActiveTrue(UUID userId);
}