package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ChatbotRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ChatbotService;
import br.com.teamss.skillswap.skill_swap.model.services.TranslationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotServiceImpl.class);
    private static final String INTERACTIONS_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/interactions";
    private static final String RETIRED_MODEL = "gemini-2.0-flash-001";

    private final TranslationService translationService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String model;

    @Value("${gemini.api.fallback-model:gemini-3.7-flash}")
    private String fallbackModel;

    public ChatbotServiceImpl(TranslationService translationService, WebClient.Builder webClientBuilder) {
        this.translationService = translationService;
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatbotResponseDTO getChatbotResponse(ChatbotRequestDTO requestDTO) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("API Key do Gemini não configurada. Chatbot indisponível.");
            return new ChatbotResponseDTO(
                "Ainda não consigo responder porque o assistente não foi configurado no servidor. A plataforma continua disponível para você.",
                null,
                false
            );
        }

        if (requestDTO == null || requestDTO.getMessage() == null || requestDTO.getMessage().isBlank()) {
            logger.warn("Recebida requisição de chatbot inválida (mensagem vazia).");
            return new ChatbotResponseDTO("Por favor, envie uma mensagem.", null, false);
        }

        try {
            String systemPrompt = "Você é um assistente prestativo da plataforma SkillSwap. "
                + "Ajude os usuários a navegar na plataforma, encontrar informações sobre habilidades, "
                + "entender as trocas e responder perguntas gerais sobre o SkillSwap. "
                + "Seja amigável, claro, conciso e responda sempre em português brasileiro. "
                + "Não invente funcionalidades que não estejam disponíveis.";

            String input = requestDTO.getMessage();
            if (requestDTO.getContext() != null && !requestDTO.getContext().isBlank()) {
                input = "Contexto anterior fornecido pelo aplicativo:\n"
                    + requestDTO.getContext()
                    + "\n\nMensagem do usuário:\n"
                    + requestDTO.getMessage();
            }

            String responseJson = callGeminiWithFallback(input, systemPrompt);
            String rawResponse = extractInteractionText(responseJson);

            if (rawResponse.isBlank()) {
                logger.warn("Resposta do Gemini não contém texto utilizável.");
                return new ChatbotResponseDTO(
                    "Não encontrei uma resposta agora. Pode tentar perguntar de outro jeito?",
                    null,
                    false
                );
            }

            String finalResponse = rawResponse;
            boolean wasSimplified = false;
            if (requestDTO.isSimplifyResponse() && translationService != null) {
                finalResponse = translationService.simplifyText(rawResponse);
                wasSimplified = !finalResponse.equals(rawResponse);
            }

            return new ChatbotResponseDTO(finalResponse, null, wasSimplified);
        } catch (WebClientResponseException e) {
            logGeminiError(e);
            return new ChatbotResponseDTO(
                "O assistente está temporariamente indisponível. Tente novamente em alguns instantes.",
                null,
                false
            );
        } catch (Exception e) {
            logger.error("Erro ao comunicar com a Interactions API do Gemini: {}", e.getMessage());
            return new ChatbotResponseDTO(
                "Tive um problema para responder agora. Tente novamente em alguns instantes.",
                null,
                false
            );
        }
    }

    private String callGeminiWithFallback(String input, String systemPrompt) throws Exception {
        Set<String> candidates = new LinkedHashSet<>();
        addModel(candidates, model);
        addModel(candidates, fallbackModel);
        addModel(candidates, "gemini-3.7-flash");
        addModel(candidates, "gemini-2.5-flash");

        WebClientResponseException lastError = null;
        for (String candidate : candidates) {
            try {
                return callInteractionWithRetry(candidate, input, systemPrompt);
            } catch (WebClientResponseException error) {
                lastError = error;
                logger.warn(
                    "O modelo Gemini '{}' recusou a interação (HTTP {}). Tentando o próximo modelo.",
                    candidate,
                    error.getStatusCode().value()
                );
            }
        }

        if (lastError != null) {
            throw lastError;
        }
        throw new IllegalStateException("Nenhum modelo Gemini disponível para esta chave.");
    }

    private void addModel(Set<String> candidates, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim().replaceFirst("^models/", "");
        if (RETIRED_MODEL.equalsIgnoreCase(normalized)) {
            logger.warn("Ignorando o modelo Gemini descontinuado configurado como fallback: {}", normalized);
            return;
        }
        candidates.add(normalized);
    }

    private String callInteractionWithRetry(String selectedModel, String input, String systemPrompt)
        throws InterruptedException {
        Map<String, Object> requestBody = Map.of(
            "model", selectedModel,
            "input", input,
            "system_instruction", systemPrompt,
            "store", false,
            "generation_config", Map.of("thinking_level", "low")
        );

        WebClientResponseException lastError = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            try {
                return webClient.post()
                    .uri(INTERACTIONS_ENDPOINT)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            } catch (WebClientResponseException error) {
                lastError = error;
                int status = error.getStatusCode().value();
                if (!isRetryable(status) || attempt == 2) {
                    throw error;
                }
                long waitMillis = 400L * (attempt + 1);
                logger.warn(
                    "Gemini respondeu HTTP {} para '{}'. Nova tentativa em {} ms.",
                    status,
                    selectedModel,
                    waitMillis
                );
                Thread.sleep(waitMillis);
            }
        }
        throw lastError;
    }

    private boolean isRetryable(int status) {
        return status == 429 || status == 500 || status == 502 || status == 503 || status == 504;
    }

    private String extractInteractionText(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        JsonNode outputText = root.path("output_text");
        if (outputText.isTextual() && !outputText.asText().isBlank()) {
            return outputText.asText();
        }

        for (JsonNode step : root.path("steps")) {
            if (!"model_output".equalsIgnoreCase(step.path("type").asText())) {
                continue;
            }
            for (JsonNode content : step.path("content")) {
                if ("text".equalsIgnoreCase(content.path("type").asText())
                    && content.path("text").isTextual()) {
                    String text = content.path("text").asText();
                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }
        return "";
    }

    private void logGeminiError(WebClientResponseException error) {
        String body = error.getResponseBodyAsString();
        if (body.length() > 500) {
            body = body.substring(0, 500);
        }
        logger.error("Gemini recusou a interação (HTTP {}): {}", error.getStatusCode().value(), body);
    }
}
