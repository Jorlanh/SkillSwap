package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonCompletionResponse;
import br.com.teamss.skillswap.skill_swap.dto.LessonCertificateResponse;
import br.com.teamss.skillswap.skill_swap.dto.LessonReviewRequest;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import java.util.List;
import java.util.UUID;

public interface LessonService {
    Lesson scheduleLesson(LessonScheduleRequestDTO lessonRequest);
    List<Lesson> getUpcomingLessons(UUID userId);
    void notifyLesson(Long lessonId);
    List<Lesson> getLessonsForUser(UUID userId);
    LessonCompletionResponse confirmCompletion(Long lessonId);
    void submitReview(Long lessonId, LessonReviewRequest request);
    LessonCertificateResponse getCertificate(Long lessonId);
}
