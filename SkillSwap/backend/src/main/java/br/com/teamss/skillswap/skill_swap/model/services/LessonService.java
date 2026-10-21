package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.LessonScheduleRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonCertificateDTO;
import br.com.teamss.skillswap.skill_swap.dto.LessonViewDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import java.util.List;
import java.util.UUID;

public interface LessonService {
    Lesson scheduleLesson(LessonScheduleRequestDTO lessonRequest);
    List<Lesson> getUpcomingLessons(UUID userId);
    List<LessonViewDTO> getMyLessons();
    LessonCertificateDTO completeLesson(Long lessonId);
    LessonCertificateDTO getCertificate(Long lessonId);
    void notifyLesson(Long lessonId);
}
