package br.com.teamss.skillswap.skill_swap.model.services;

public interface TranslationService {
    /**
     * Traduz o texto para o idioma de destino especificado.
     * @param text O texto a ser traduzido.
     * @param targetLanguage O código do idioma de destino (ex: "en", "es").
     * @return O texto traduzido ou o texto original em caso de erro.
     */
    String translate(String text, String targetLanguage);

    /**
     * Simplifica o texto fornecido para uma linguagem mais fácil de entender.
     * @param text O texto a ser simplificado.
     * @return O texto simplificado ou o texto original em caso de erro.
     */
    String simplifyText(String text);
}
