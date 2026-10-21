package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.ModerationAssessment;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * A IA faz triagem de conteúdo. Ela não possui permissão para suspender ou
 * banir contas; essa decisão é exclusiva do fluxo administrativo humano.
 */
@Service
public class GeminiApiModerationService implements ContentModerationService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiApiModerationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ModerationAssessment assess(String text) {
        if (text == null || text.isBlank()) {
            return new ModerationAssessment("EMPTY", 1.0, "ALLOW", false, "Texto vazio.");
        }
        if (apiKey == null || apiKey.isBlank()) {
            return unavailableAssessment("Chave Gemini não configurada; conteúdo contido para revisão.");
        }

        try {
            String prompt = "Você é um classificador de segurança da plataforma SkillSwap. "
                    + "Analise o texto para discurso de ódio, assédio, violência, conteúdo sexual, ameaça, spam "
                    + "ou incentivo à automutilação. Não aplique punição. Responda somente JSON válido com as chaves "
                    + "category, confidence (0 a 1), recommendation (ALLOW, REVIEW ou BLOCK), blocked (true/false) "
                    + "e explanation. Use REVIEW quando houver dúvida. Texto: " + text;

            String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                    + model + ":generateContent";
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

            JsonNode root = objectMapper.readTree(responseJson);
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return unavailableAssessment("A resposta da IA não trouxe uma classificação válida.");
            }
            String raw = candidates.get(0).path("content").path("parts").get(0)
                    .path("text").asText("").trim();
            JsonNode result = objectMapper.readTree(extractJson(raw));
            String category = result.path("category").asText("UNSPECIFIED");
            double confidence = result.path("confidence").asDouble(0.0);
            String recommendation = result.path("recommendation").asText("REVIEW").toUpperCase();
            boolean blocked = result.path("blocked").asBoolean(!"ALLOW".equals(recommendation));
            String explanation = result.path("explanation").asText("Classificação automática enviada para revisão.");
            return new ModerationAssessment(category, confidence, recommendation, blocked, explanation);
        } catch (Exception exception) {
            // Fail-closed para o conteúdo, mas sem punição de conta. O caso
            // continua elegível para revisão humana pelo painel.
            System.err.println("Falha na triagem Gemini; conteúdo colocado em contenção: "
                    + exception.getMessage());
            return unavailableAssessment("Serviço de moderação indisponível; revisão humana necessária.");
        }
    }

    @Override
    public boolean isContentInappropriate(String text) {
        return assess(text).blocked();
    }

    private ModerationAssessment unavailableAssessment(String explanation) {
        return new ModerationAssessment("MODERATION_UNAVAILABLE", 0.0, "REVIEW", true, explanation);
    }

    private String extractJson(String raw) {
        String normalized = raw.replace("```json", "").replace("```", "").trim();
        int start = normalized.indexOf('{');
        int end = normalized.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return normalized.substring(start, end + 1);
        }
        return normalized;
    }
}
