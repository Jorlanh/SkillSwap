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

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    @Value("${gemini.api.fallback-model:gemini-2.5-flash}")
    private String fallbackModel;

    @Value("${gemini.api.timeout-seconds:10}")
    private long timeoutSeconds;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.api.model:llama-3.1-8b-instant}")
    private String groqModel;

    @Value("${groq.api.url:https://api.groq.com/openai/v1/chat/completions}")
    private String groqApiUrl;

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
         if (isBlank(apiKey) && isBlank(groqApiKey)) {
            logger.error("Gemini e Groq não estão configuradas para tradução/simplificação.");
            return fallbackText;
        }

        if (isBlank(apiKey)) {
            try {
                JsonNode rootNode = objectMapper.readTree(callGroq(prompt));
                String groqResult = rootNode.path("choices").get(0)
                    .path("message").path("content").asText("").trim();
                return groqResult.isBlank() ? fallbackText : groqResult;
            } catch (Exception fallbackError) {
                logger.error("Groq não conseguiu processar a tradução/simplificação: {}", fallbackError.getMessage());
                return fallbackText;
            }
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
             logger.warn("Gemini indisponível para tradução/simplificação. Tentando Groq: {}", e.getMessage());
             if (!isBlank(groqApiKey)) {
                 try {
                     JsonNode rootNode = objectMapper.readTree(callGroq(prompt));
                     String groqResult = rootNode.path("choices").get(0)
                         .path("message").path("content").asText("").trim();
                     if (!groqResult.isBlank()) return groqResult;
                 } catch (Exception fallbackError) {
                     logger.error("Groq também não conseguiu processar a tradução/simplificação: {}", fallbackError.getMessage());
                 }
             }
             return fallbackText;
        }
    }

    private String callGemini(String selectedModel, Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + selectedModel + ":generateContent";
        return webClient.post().uri(url)
                .header("x-goog-api-key", apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody).retrieve().bodyToMono(String.class).block(requestTimeout());
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
            } catch (IllegalStateException error) {
                logger.warn("Timeout ou resposta inválida do modelo Gemini '{}'. Tentando o próximo modelo.", candidate);
            }
        }
        try {
            String discovered = resolveAvailableModel();
            if (discovered != null && candidates.add(discovered)) return callGemini(discovered, requestBody);
        } catch (Exception error) {
            logger.warn("Não foi possível descobrir outro modelo Gemini: {}", error.getMessage());
        }
        if (lastError != null) throw lastError;
        throw new IllegalStateException("Nenhum modelo Gemini disponível para tradução.");
    }

    private String callGroq(String prompt) {
        Map<String, Object> requestBody = Map.of(
            "model", groqModel,
            "messages", List.of(Map.of(
                "role", "user",
                "content", prompt
            )),
            "temperature", 0.2
        );

        String response = webClient.post()
            .uri(groqApiUrl)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + groqApiKey)
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .bodyValue(requestBody)
            .retrieve()
            .bodyToMono(String.class)
            .block(requestTimeout());

        if (response == null || response.isBlank()) {
            throw new IllegalStateException("Groq retornou uma resposta vazia.");
        }
        return response;
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
        for (String preferred : Arrays.asList("gemini-2.5-flash", "gemini-2.5-flash-lite", model, fallbackModel, "gemini-3.8-flash", "gemini-3.7-flash", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b")) {
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private java.time.Duration requestTimeout() {
        return java.time.Duration.ofSeconds(Math.max(5, timeoutSeconds));
    }
}
