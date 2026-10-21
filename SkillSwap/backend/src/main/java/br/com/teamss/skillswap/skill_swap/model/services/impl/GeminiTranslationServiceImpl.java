package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Service
public class GeminiTranslationServiceImpl implements TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiTranslationServiceImpl.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiTranslationServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public String translate(String text, String targetLanguage) {
        if (text == null || text.isBlank() || targetLanguage == null || targetLanguage.isBlank()) {
            logger.warn("Texto ou idioma de destino inválido para tradução.");
            return text; 
        }
        String prompt = String.format("Translate the following text to %s: \"%s\"", targetLanguage, text);
        return callGemini(prompt, text);
    }

    @Override
    public String simplifyText(String text) {
        if (text == null || text.isBlank()) {
             logger.warn("Texto inválido para simplificação.");
            return text; 
        }
        String prompt = String.format(
            "Reescreva o seguinte texto em uma linguagem simples e fácil de entender, " +
            "ideal para pessoas com dificuldades de leitura ou aprendizado. " +
            "Use frases curtas, vocabulário comum e evite jargões: \"%s\"", text
        );
        return callGemini(prompt, text); 
    }

    private String callGemini(String prompt, String fallbackText) {
         if (apiKey == null || apiKey.isBlank()) {
            logger.error("API Key do Gemini não está configurada.");
            return fallbackText;
        }

        try {
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );

            String responseJson = webClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String result = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().trim();
            return result;

        } catch (Exception e) {
             logger.error("Erro inesperado ao interagir com a API Gemini REST: {}", e.getMessage());
             return fallbackText;
        }
    }
}
