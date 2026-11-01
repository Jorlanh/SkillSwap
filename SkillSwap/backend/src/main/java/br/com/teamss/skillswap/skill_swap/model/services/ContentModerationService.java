package br.com.teamss.skillswap.skill_swap.model.services;

/**
 * Interface para o serviço de moderação de conteúdo.
 */
public interface ContentModerationService {
    /**
     * Analisa o texto e retorna true se for considerado inapropriado.
     * @param text O texto a ser analisado.
     * @return true se o conteúdo violar as diretrizes, false caso contrário.
     */
    boolean isContentInappropriate(String text);
}