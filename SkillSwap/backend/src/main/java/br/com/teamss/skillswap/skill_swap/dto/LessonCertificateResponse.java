package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;

public record LessonCertificateResponse(
    String certificateId,
    String studentName,
    String teacherName,
    String lessonTitle,
    Integer durationMinutes,
    Instant completedAt
) {}
