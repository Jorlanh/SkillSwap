package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Story;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;

public interface StoryRepository extends JpaRepository<Story, Long> {
    List<Story> findByExpiresAtAfterOrderByCreatedAtDesc(Instant now);
}
