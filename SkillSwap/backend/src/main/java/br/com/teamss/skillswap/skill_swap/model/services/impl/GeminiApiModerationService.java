package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Moderação textual com Gemini como principal e Groq como fallback. */
@Service
public class GeminiApiModerationService implements ContentModerationService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiApiModerationService.class);
    private static final String GEMINI_ENDPOINT = "https://generativelanguage.googleapis.com/v1beta/models/";

    @Value("${gemini.api.key:}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String model;

    @Value("${gemini.api.fallback-model:gemini-3.8-flash}")
    private String fallbackModel;

    @Value("${gemini.api.timeout-seconds:10}")
    private long timeoutSeconds;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.model:openai/gpt-oss-20b}")
    private String groqModel;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiApiModerationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isContentInappropriate(String text) {
        if (text == null || text.isBlank()) return false;

        String prompt = moderationPrompt(text);
        if (!isBlank(apiKey)) {
            try {
                return startsWithYes(extractGeminiText(callGeminiWithFallback(prompt)));
            } catch (Exception error) {
                logger.warn("Gemini indisponível para moderação; ativando Groq: {}", error.getMessage());
            }
        }

        if (!isBlank(groqApiKey)) {
            try {
                return startsWithYes(extractGroqText(callGroq(prompt)));
            } catch (Exception error) {
                logger.error("Gemini e Groq falharam na moderação do conteúdo: {}", error.getMessage());
            }
        } else {
            logger.error("Nenhum provedor de moderação está configurado. GEMINI_API_KEY e GROQ_API_KEY estão vazias.");
        }

        // Indisponibilidade do provedor não aplica punição automática.
        return false;
    }

    private String moderationPrompt(String text) {
        return "Você é um moderador de conteúdo da plataforma SkillSwap. "
            + "Classifique como SIM somente se houver racismo, xenofobia, homofobia, transfobia, "
            + "discriminação contra deficiência, perseguição, assédio abusivo, ameaça real, incentivo à violência, "
            + "conteúdo sexual envolvendo menores, exposição de dados pessoais, spam abusivo ou incentivo à automutilação. "
            + "Não bloqueie críticas, opiniões políticas, debates, humor, linguagem coloquial ou discordância respeitosa "
            + "apenas por serem controversos. Responda somente SIM ou NAO. Texto: \"" + text;
    }

    private String callGeminiWithFallback(String prompt) throws Exception {
        Set<String> candidates = new LinkedHashSet<>();
        addModel(candidates, model);
        addModel(candidates, fallbackModel);
        addModel(candidates, "gemini-3.8-flash");
        addModel(candidates, "gemini-3.7-flash");
        addModel(candidates, "gemini-2.5-flash");

        Exception lastError = null;
        for (String candidate : candidates) {
            try {
                Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
                );
                String response = webClient.post()
                    .uri(GEMINI_ENDPOINT + candidate + ":generateContent")
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(requestTimeout());
                if (response != null && !response.isBlank()) return response;
                lastError = new IllegalStateException("Resposta Gemini vazia.");
            } catch (Exception error) {
                lastError = error;
                logger.warn("Modelo Gemini '{}' falhou na moderação: {}", candidate, error.getMessage());
            }
        }
        throw lastError == null
            ? new IllegalStateException("Nenhum modelo Gemini disponível para moderação.")
            : lastError;
    }

    private String callGroq(String prompt) {
        Map<String, Object> body = Map.of(
            "model", groqModel,
            "messages", List.of(
                Map.of("role", "system", "content", "Responda exclusivamente SIM ou NAO."),
                Map.of("role", "user", "content", prompt)
            ),
            "temperature", 0.0
        );
        String response = webClient.post()
            .uri(groqApiUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(body)
            .retrieve()
            .bodyToMono(String.class)
            .block(requestTimeout());
        if (response == null || response.isBlank()) throw new IllegalStateException("Resposta Groq vazia.");
        return response;
    }

    private String extractGeminiText(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        return root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("").trim();
    }

    private String extractGroqText(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        return root.path("choices").path(0).path("message").path("content").asText("").trim();
    }

    private void addModel(Set<String> candidates, String value) {
        if (isBlank(value)) return;
        String normalized = value.trim().replaceFirst("^models/", "");
        if (!"gemini-2.0-flash-001".equalsIgnoreCase(normalized)) candidates.add(normalized);
    }

    private boolean startsWithYes(String value) {
        return value != null && value.trim().toUpperCase().startsWith("SIM");
    }

    private Duration requestTimeout() {
        return Duration.ofSeconds(Math.max(5, timeoutSeconds));
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
