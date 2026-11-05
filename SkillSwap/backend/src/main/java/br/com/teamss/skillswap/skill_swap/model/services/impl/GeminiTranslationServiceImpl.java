package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.TranslationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Service
public class GeminiTranslationServiceImpl implements TranslationService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiTranslationServiceImpl.class);

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String model;

    @Value("${gemini.api.fallback-model:gemini-3.7-flash}")
    private String fallbackModel;

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
            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );

            String responseJson = callGeminiWithFallback(requestBody);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String result = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().trim();
            return result;

        } catch (Exception e) {
             logger.error("Erro inesperado ao interagir com a API Gemini REST: {}", e.getMessage());
             return fallbackText;
        }
    }

    private String callGemini(String selectedModel, Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + selectedModel + ":generateContent";
        return webClient.post().uri(url)
                .header("x-goog-api-key", apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody).retrieve().bodyToMono(String.class).block();
    }

    private String callGeminiWithFallback(Map<String, Object> requestBody) throws Exception {
        Set<String> candidates = new LinkedHashSet<>();
        addModel(candidates, model);
        addModel(candidates, fallbackModel);
        addModel(candidates, "gemini-3.8-flash");
        WebClientResponseException lastError = null;
        for (String candidate : candidates) {
            try {
                return callGemini(candidate, requestBody);
            } catch (WebClientResponseException error) {
                lastError = error;
                logger.warn("O modelo Gemini '{}' não está disponível (HTTP {}). Tentando o próximo modelo.", candidate, error.getStatusCode().value());
            }
        }
        String discovered = resolveAvailableModel();
        if (discovered != null && candidates.add(discovered)) return callGemini(discovered, requestBody);
        if (lastError != null) throw lastError;
        throw new IllegalStateException("Nenhum modelo Gemini disponível para tradução.");
    }

    private void addModel(Set<String> candidates, String value) {
        if (value == null || value.isBlank()) return;
        String normalized = value.replaceFirst("^models/", "");
        if ("gemini-2.0-flash-001".equalsIgnoreCase(normalized)) return;
        candidates.add(normalized);
    }

    private String resolveAvailableModel() throws Exception {
        String response = webClient.get()
                .uri("https://generativelanguage.googleapis.com/v1beta/models")
                .header("x-goog-api-key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonNode models = objectMapper.readTree(response).path("models");
        for (String preferred : Arrays.asList("gemini-3.8-flash", "gemini-3.7-flash", "gemini-2.5-flash", model, fallbackModel, "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b", "gemini-2.5-flash-lite")) {
            for (JsonNode item : models) {
                String name = item.path("name").asText("").replaceFirst("^models/", "");
                if (preferred.equals(name) && supportsGeneration(item)) return name;
            }
        }
        for (JsonNode item : models) {
            String name = item.path("name").asText("").replaceFirst("^models/", "");
            if (name.contains("flash") && supportsGeneration(item)) return name;
        }
        throw new IllegalStateException("Nenhum modelo Gemini com generateContent está disponível para esta chave.");
    }

    private boolean supportsGeneration(JsonNode modelNode) {
        for (JsonNode method : modelNode.path("supportedGenerationMethods")) {
            if ("generateContent".equalsIgnoreCase(method.asText())) return true;
        }
        return false;
    }
}
