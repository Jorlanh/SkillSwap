package br.com.teamss.skillswap.skill_swap.model.services;

/**
 * Interface para o serviço de moderação de conteúdo.
 */
public interface ContentModerationService {
    /**
     * Classifica o conteúdo e informa se ele deve ser contido enquanto aguarda
     * revisão. A classificação automática nunca aplica suspensão ou banimento.
     */
    default ModerationAssessment assess(String text) {
        boolean blocked = isContentInappropriate(text);
        return new ModerationAssessment(
                blocked ? "UNSAFE" : "SAFE",
                blocked ? 1.0 : 1.0,
                blocked ? "REVIEW" : "ALLOW",
                blocked,
                blocked ? "Conteúdo contido para revisão humana." : "Nenhuma violação identificada."
        );
    }

    /**
     * Analisa o texto e retorna true se for considerado inapropriado.
     * @param text O texto a ser analisado.
     * @return true se o conteúdo violar as diretrizes, false caso contrário.
     */
    boolean isContentInappropriate(String text);
}
