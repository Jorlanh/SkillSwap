package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;

/** Dados verificáveis usados para montar o certificado no navegador do participante. */
public record LessonCertificateDTO(
        Long lessonId,
        String certificateCode,
        String studentName,
        String studentUsername,
        String teacherName,
        String title,
        String introduction,
        int durationMinutes,
        Instant completedAt
) {}
