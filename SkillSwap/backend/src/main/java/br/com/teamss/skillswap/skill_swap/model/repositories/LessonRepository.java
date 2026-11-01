package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    // ADICIONE ESTE MÉTODO:
    List<Lesson> findByTeacher_UserIdOrStudent_UserIdAndScheduledTimeAfter(UUID teacherId, UUID studentId, Instant currentTime);
    List<Lesson> findByTeacher_UserIdOrStudent_UserIdOrderByScheduledTimeDesc(UUID teacherId, UUID studentId);

}