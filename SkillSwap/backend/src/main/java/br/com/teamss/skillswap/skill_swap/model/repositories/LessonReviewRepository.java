package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.LessonReview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LessonReviewRepository extends JpaRepository<LessonReview, Long> {
    boolean existsByLessonLessonIdAndReviewerUserId(Long lessonId, UUID reviewerId);
}
