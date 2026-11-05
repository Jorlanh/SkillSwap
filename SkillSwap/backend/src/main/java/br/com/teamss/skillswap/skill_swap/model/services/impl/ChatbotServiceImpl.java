
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
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private static final Logger logger =
        LoggerFactory.getLogger(ChatbotServiceImpl.class);

    private static final String INTERACTIONS_ENDPOINT =
        "https://generativelanguage.googleapis.com/v1beta/interactions";

    private static final String RETIRED_MODEL =
        "gemini-2.0-flash-001";

    // Configurações de resiliência
    private static final int MAX_ATTEMPTS = 3;

    private static final long INITIAL_WAIT_MS = 1000L;

    private static final long MAX_JITTER_MS = 500L;

    private static final Duration REQUEST_TIMEOUT =
        Duration.ofSeconds(30);

    private final TranslationService translationService;

    private final WebClient webClient;

    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String model;

    @Value("${gemini.api.fallback-model:gemini-3.7-flash}")
    private String fallbackModel;

    public ChatbotServiceImpl(
        TranslationService translationService,
        WebClient.Builder webClientBuilder
    ) {
        this.translationService = translationService;
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    // =========================================================
    // MÉTODO PRINCIPAL
    // =========================================================

    @Override
    public ChatbotResponseDTO getChatbotResponse(
        ChatbotRequestDTO requestDTO
    ) {

        if (apiKey == null || apiKey.isBlank()) {

            logger.error(
                "API Key do Gemini não configurada. Chatbot indisponível."
            );

            return new ChatbotResponseDTO(
                "Ainda não consigo responder porque o assistente não foi configurado no servidor. A plataforma continua disponível para você.",
                null,
                false
            );
        }

        if (
            requestDTO == null
            || requestDTO.getMessage() == null
            || requestDTO.getMessage().isBlank()
        ) {

            logger.warn(
                "Recebida requisição de chatbot inválida (mensagem vazia)."
            );

            return new ChatbotResponseDTO(
                "Por favor, envie uma mensagem.",
                null,
                false
            );
        }

        try {

            String systemPrompt =
                "Você é um assistente prestativo da plataforma SkillSwap. "
                + "Ajude os usuários a navegar na plataforma, encontrar informações sobre habilidades, "
                + "entender as trocas e responder perguntas gerais sobre o SkillSwap. "
                + "Seja amigável, claro, conciso e responda sempre em português brasileiro. "
                + "Não invente funcionalidades que não estejam disponíveis.";

            String input = requestDTO.getMessage();

            if (
                requestDTO.getContext() != null
                && !requestDTO.getContext().isBlank()
            ) {

                input =
                    "Contexto anterior fornecido pelo aplicativo:\n"
                    + requestDTO.getContext()
                    + "\n\nMensagem do usuário:\n"
                    + requestDTO.getMessage();
            }

            // Comunicação com Gemini e fallback automático.
            String responseJson = callGeminiWithFallback(
                input,
                systemPrompt
            );

            String rawResponse = extractInteractionText(responseJson);

            if (rawResponse.isBlank()) {

                logger.warn(
                    "Resposta do Gemini não contém texto utilizável."
                );

                return new ChatbotResponseDTO(
                    "Não encontrei uma resposta agora. Pode tentar perguntar de outro jeito?",
                    null,
                    false
                );
            }

            // Simplificação da resposta, quando solicitada.
            String finalResponse = rawResponse;

            boolean wasSimplified = false;

            if (
                requestDTO.isSimplifyResponse()
                && translationService != null
            ) {

                try {

                    String simplifiedResponse =
                        translationService.simplifyText(rawResponse);

                    if (
                        simplifiedResponse != null
                        && !simplifiedResponse.isBlank()
                    ) {

                        finalResponse = simplifiedResponse;

                        wasSimplified =
                            !finalResponse.equals(rawResponse);
                    }

                } catch (Exception e) {

                    logger.warn(
                        "Não foi possível simplificar a resposta. Utilizando texto original: {}",
                        e.getMessage()
                    );

                    // Mantém a resposta original do Gemini.
                    finalResponse = rawResponse;
                    wasSimplified = false;
                }
            }

            return new ChatbotResponseDTO(
                finalResponse,
                null,
                wasSimplified
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            logger.warn(
                "Comunicação com Gemini interrompida."
            );

            return new ChatbotResponseDTO(
                "A solicitação foi interrompida. Tente novamente.",
                null,
                false
            );

        } catch (WebClientResponseException e) {

            logGeminiError(e);

            return new ChatbotResponseDTO(
                "O assistente está temporariamente indisponível. Tente novamente em alguns instantes.",
                null,
                false
            );

        } catch (WebClientRequestException e) {

            logger.error(
                "Falha de conexão com a API do Gemini: {}",
                e.getMessage()
            );

            return new ChatbotResponseDTO(
                "Não foi possível conectar ao assistente neste momento. Verifique sua conexão e tente novamente.",
                null,
                false
            );

        } catch (Exception e) {

            logger.error(
                "Erro inesperado ao comunicar com a Interactions API do Gemini.",
                e
            );

            return new ChatbotResponseDTO(
                "Tive um problema para responder agora. Tente novamente em alguns instantes.",
                null,
                false
            );
        }
    }

    // =========================================================
    // FALLBACK ENTRE MODELOS
    // =========================================================

    private String callGeminiWithFallback(
        String input,
        String systemPrompt
    ) throws Exception {

        Set<String> candidates = new LinkedHashSet<>();

        // Primeiro modelo configurado.
        addModel(candidates, model);

        // Modelo alternativo configurado.
        addModel(candidates, fallbackModel);

        if (candidates.isEmpty()) {

            throw new IllegalStateException(
                "Nenhum modelo Gemini disponível para esta chave."
            );
        }

        WebClientResponseException lastHttpError = null;

        WebClientRequestException lastConnectionError = null;

        for (String candidate : candidates) {

            try {

                logger.info(
                    "Tentando utilizar modelo Gemini: {}",
                    candidate
                );

                return callInteractionWithRetry(
                    candidate,
                    input,
                    systemPrompt
                );

            } catch (WebClientResponseException error) {

                lastHttpError = error;

                int status = error.getStatusCode().value();

                // Erros que justificam tentar outro modelo.
                if (!shouldTryNextModel(status)) {

                    logger.error(
                        "Erro HTTP {} não permite fallback automático para outro modelo.",
                        status
                    );

                    throw error;
                }

                logger.warn(
                    "Modelo Gemini '{}' indisponível (HTTP {}). Tentando próximo modelo.",
                    candidate,
                    status
                );

            } catch (WebClientRequestException error) {

                lastConnectionError = error;

                logger.warn(
                    "Falha de conexão com o modelo '{}': {}. Tentando próximo modelo.",
                    candidate,
                    error.getMessage()
                );
            }
        }

        // Retorna o último erro HTTP, caso todos os modelos falhem.
        if (lastHttpError != null) {
            throw lastHttpError;
        }

        // Caso as falhas tenham sido exclusivamente de conexão.
        if (lastConnectionError != null) {
            throw lastConnectionError;
        }

        throw new IllegalStateException(
            "Nenhum modelo Gemini conseguiu processar a solicitação."
        );
    }

    // =========================================================
    // ADIÇÃO E VALIDAÇÃO DOS MODELOS
    // =========================================================

    private void addModel(
        Set<String> candidates,
        String value
    ) {

        if (value == null || value.isBlank()) {
            return;
        }

        String normalized = value.trim()
            .replaceFirst("^models/", "");

        if (RETIRED_MODEL.equalsIgnoreCase(normalized)) {

            logger.warn(
                "Ignorando modelo Gemini descontinuado: {}",
                normalized
            );

            return;
        }

        candidates.add(normalized);
    }

    // =========================================================
    // RETRY COM EXPONENTIAL BACKOFF E JITTER
    // =========================================================

    private String callInteractionWithRetry(
        String selectedModel,
        String input,
        String systemPrompt
    ) throws InterruptedException {

        Map<String, Object> requestBody = Map.of(
            "model", selectedModel,
            "input", input,
            "system_instruction", systemPrompt,
            "store", false,
            "generation_config", Map.of(
                "thinking_level", "low"
            )
        );

        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {

            try {

                String response = webClient.post()
                    .uri(INTERACTIONS_ENDPOINT)
                    .header("x-goog-api-key", apiKey)
                    .header(
                        HttpHeaders.CONTENT_TYPE,
                        MediaType.APPLICATION_JSON_VALUE
                    )
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block(REQUEST_TIMEOUT);

                if (response == null || response.isBlank()) {

                    throw new IllegalStateException(
                        "Gemini retornou uma resposta vazia."
                    );
                }

                return response;

            } catch (WebClientResponseException error) {

                int status = error.getStatusCode().value();

                if (
                    !isRetryable(status)
                    || attempt == MAX_ATTEMPTS - 1
                ) {

                    throw error;
                }

                waitBeforeRetry(
                    selectedModel,
                    status,
                    attempt
                );

            } catch (WebClientRequestException error) {

                if (attempt == MAX_ATTEMPTS - 1) {
                    throw error;
                }

                logger.warn(
                    "Falha de conexão com Gemini '{}'. Tentativa {}/{}. {}",
                    selectedModel,
                    attempt + 1,
                    MAX_ATTEMPTS,
                    error.getMessage()
                );

                waitBeforeRetry(
                    selectedModel,
                    0,
                    attempt
                );
            }
        }

        throw new IllegalStateException(
            "Não foi possível obter resposta do modelo Gemini: "
                + selectedModel
        );
    }

    // =========================================================
    // INTERVALO ENTRE TENTATIVAS
    // =========================================================

    private void waitBeforeRetry(
        String selectedModel,
        int status,
        int attempt
    ) throws InterruptedException {

        long baseWait =
            INITIAL_WAIT_MS * (1L << attempt);

        long jitter = ThreadLocalRandom.current()
            .nextLong(MAX_JITTER_MS + 1);

        long waitMillis = baseWait + jitter;

        logger.warn(
            "Gemini respondeu HTTP {} para '{}'. Tentativa {}/{}. Nova tentativa em {} ms.",
            status == 0 ? "CONEXÃO" : status,
            selectedModel,
            attempt + 1,
            MAX_ATTEMPTS,
            waitMillis
        );

        Thread.sleep(waitMillis);
    }

    // =========================================================
    // CLASSIFICAÇÃO DE ERROS
    // =========================================================

    private boolean isRetryable(int status) {

        return status == 408
            || status == 429
            || status == 500
            || status == 502
            || status == 503
            || status == 504;
    }

    private boolean shouldTryNextModel(int status) {

        return status == 404
            || status == 408
            || status == 429
            || status == 500
            || status == 502
            || status == 503
            || status == 504;
    }

    // =========================================================
    // EXTRAÇÃO DO TEXTO DA RESPOSTA
    // =========================================================

    private String extractInteractionText(
        String responseJson
    ) throws Exception {

        JsonNode root = objectMapper.readTree(responseJson);

        JsonNode outputText = root.path("output_text");

        if (
            outputText.isTextual()
            && !outputText.asText().isBlank()
        ) {

            return outputText.asText();
        }

        for (JsonNode step : root.path("steps")) {

            if (
                !"model_output".equalsIgnoreCase(
                    step.path("type").asText()
                )
            ) {
                continue;
            }

            for (JsonNode content : step.path("content")) {

                if (
                    "text".equalsIgnoreCase(
                        content.path("type").asText()
                    )
                    && content.path("text").isTextual()
                ) {

                    String text = content.path("text").asText();

                    if (!text.isBlank()) {
                        return text;
                    }
                }
            }
        }

        return "";
    }

    // =========================================================
    // LOG DE ERROS DO GEMINI
    // =========================================================

    private void logGeminiError(
        WebClientResponseException error
    ) {

        String body = error.getResponseBodyAsString();

        if (body.length() > 500) {
            body = body.substring(0, 500);
        }

        logger.error(
            "Erro na API Gemini (HTTP {}): {}",
            error.getStatusCode().value(),
            body
        );
    }
}
