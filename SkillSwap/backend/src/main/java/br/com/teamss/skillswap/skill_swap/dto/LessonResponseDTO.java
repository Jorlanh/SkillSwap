package br.com.teamss.skillswap.skill_swap.dto;

import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import br.com.teamss.skillswap.skill_swap.model.entities.User;

import java.time.Instant;
import java.util.UUID;

/**
 * Resposta segura da agenda. A entidade Lesson possui relacionamentos JPA com
 * User e não deve ser serializada diretamente, pois isso pode criar ciclos ou
 * disparar carregamentos lazy durante a resposta HTTP.
 */
public record LessonResponseDTO(
        Long lessonId,
        String title,
        Instant scheduledTime,
        Integer durationMinutes,
        String status,
        boolean teacherConfirmed,
        boolean studentConfirmed,
        Instant completedAt,
        ParticipantDTO teacher,
        ParticipantDTO student) {

    public static LessonResponseDTO from(Lesson lesson) {
        return new LessonResponseDTO(
                lesson.getLessonId(),
                lesson.getTitle(),
                lesson.getScheduledTime(),
                lesson.getDurationMinutes(),
                lesson.getStatus(),
                lesson.isTeacherConfirmed(),
                lesson.isStudentConfirmed(),
                lesson.getCompletedAt(),
                ParticipantDTO.from(lesson.getTeacher()),
                ParticipantDTO.from(lesson.getStudent()));
    }

    public record ParticipantDTO(UUID userId, String username, String name) {
        private static ParticipantDTO from(User user) {
            if (user == null) {
                return null;
            }
            return new ParticipantDTO(user.getUserId(), user.getUsername(), user.getName());
        }
    }
}
