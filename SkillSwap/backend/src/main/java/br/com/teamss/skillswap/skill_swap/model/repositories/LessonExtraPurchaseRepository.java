package br.com.teamss.skillswap.skill_swap.model.repositories;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import br.com.teamss.skillswap.skill_swap.model.entities.LessonExtraPurchase;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LessonExtraPurchaseRepository extends JpaRepository<LessonExtraPurchase, UUID> {
    boolean existsByIdempotencyKey(String idempotencyKey);
    long countByStatus(LessonExtraPurchase.Status status);
    @Query("select coalesce(sum(p.amount), 0) from LessonExtraPurchase p where p.status = :status")
    long sumPaidAmount(@Param("status") LessonExtraPurchase.Status status);
}
