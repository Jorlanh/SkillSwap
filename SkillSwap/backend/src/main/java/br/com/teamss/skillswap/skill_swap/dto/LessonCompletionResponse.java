package br.com.teamss.skillswap.skill_swap.dto;

public record LessonCompletionResponse(
    Long lessonId,
    String status,
    boolean confirmedByCurrentUser,
    boolean readyForCertificate
) {}
