package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;

/** Visão segura de uma aula para a agenda do usuário autenticado. */
public record LessonViewDTO(
        Long lessonId,
        String teacherName,
        String studentName,
        Instant scheduledTime,
        String status,
        String title,
        String introduction,
        int durationMinutes,
        Instant completedAt,
        boolean certificateAvailable
) {}
