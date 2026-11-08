package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface LessonRepository extends JpaRepository<Lesson, Long> {

    @Query("select l from Lesson l where (l.teacher.userId = :userId or l.student.userId = :userId) "
            + "and l.scheduledTime > :currentTime order by l.scheduledTime asc")
    List<Lesson> findUpcomingForUser(@Param("userId") UUID userId,
                                     @Param("currentTime") Instant currentTime);

    @Query("select l from Lesson l where l.teacher.userId = :userId or l.student.userId = :userId "
            + "order by l.scheduledTime desc")
    List<Lesson> findAllForUser(@Param("userId") UUID userId);

}
