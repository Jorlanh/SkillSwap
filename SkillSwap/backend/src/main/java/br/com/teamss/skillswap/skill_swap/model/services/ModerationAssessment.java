package br.com.teamss.skillswap.skill_swap.model.services;

/** Resultado da triagem automática. Não representa uma decisão disciplinar. */
public record ModerationAssessment(
        String category,
        double confidence,
        String recommendation,
        boolean blocked,
        String explanation
) {
}
