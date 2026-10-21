package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.KarmaEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KarmaEventRepository extends JpaRepository<KarmaEvent, Long> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    List<KarmaEvent> findTop20ByUser_UserIdOrderByCreatedAtDesc(UUID userId);
    @Query("select coalesce(sum(e.points), 0) from KarmaEvent e")
    long sumPoints();
}
