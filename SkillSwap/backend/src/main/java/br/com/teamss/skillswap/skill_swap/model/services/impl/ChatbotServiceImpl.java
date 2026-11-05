package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.ChatbotRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.ChatbotResponseDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ChatbotService;
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

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotServiceImpl.class);
    private final TranslationService translationService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String model;

    @Value("${gemini.api.fallback-model:gemini-2.0-flash-001}")
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
            return new ChatbotResponseDTO("Ainda não consigo responder porque o assistente não foi configurado no servidor. A plataforma continua disponível para você.", null, false);
        }
        if (requestDTO == null || requestDTO.getMessage() == null || requestDTO.getMessage().isBlank()) {
             logger.warn("Recebida requisição de chatbot inválida (mensagem vazia).");
             return new ChatbotResponseDTO("Por favor, envie uma mensagem.", null, false);
        }

        try {
            String systemPrompt = "Você é um assistente prestativo da plataforma SkillSwap. Seu objetivo é ajudar os usuários a navegar na plataforma, encontrar informações sobre habilidades, entender como funcionam as trocas e responder perguntas gerais sobre o SkillSwap. Seja amigável, claro e conciso. Responda sempre em português brasileiro.";

            Map<String, Object> requestBody = new HashMap<>();
            // A API REST do Gemini usa camelCase neste campo. O nome com
            // underscore é aceito em alguns exemplos antigos, mas não é
            // interpretado de forma consistente nas versões atuais.
            requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
            requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", requestDTO.getMessage())))));

            String responseJson = callGeminiWithFallback(requestBody);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode candidatesNode = rootNode.path("candidates");

            String rawResponse = "";
            if (!candidatesNode.isMissingNode() && candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                 rawResponse = candidatesNode.get(0).path("content").path("parts").get(0).path("text").asText();
            }

            if (rawResponse.isBlank()) {
                logger.warn("Resposta do Gemini não contém texto utilizável.");
                return new ChatbotResponseDTO("Não encontrei uma resposta agora. Pode tentar perguntar de outro jeito?", null, false);
            }

            String finalResponse = rawResponse;
            boolean wasSimplified = false;

            if (requestDTO.isSimplifyResponse() && translationService != null) {
                finalResponse = translationService.simplifyText(rawResponse);
                wasSimplified = !finalResponse.equals(rawResponse);
            }

            return new ChatbotResponseDTO(finalResponse, null, wasSimplified);

        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();
            if (body.length() > 300) body = body.substring(0, 300);
            logger.error("Gemini recusou a requisição (HTTP {}): {}", e.getStatusCode().value(), body);
            return new ChatbotResponseDTO("O assistente está temporariamente indisponível. Tente novamente em alguns instantes.", null, false);
        } catch (Exception e) {
            logger.error("Erro ao comunicar com a API do Google AI Studio: {}", e.getMessage());
            return new ChatbotResponseDTO("Tive um problema para responder agora. Tente novamente em alguns instantes.", null, false);
        }
    }

    private String callGeminiWithFallback(Map<String, Object> requestBody) throws Exception {
        Set<String> candidates = new LinkedHashSet<>();
        addModel(candidates, model);
        addModel(candidates, fallbackModel);
        // Modelo indicado pela documentação atual e também pela resposta
        // 404 recebida no Railway para gemini-2.5-flash.
        addModel(candidates, "gemini-3.8-flash");
        addModel(candidates, "gemini-2.0-flash-001");

        WebClientResponseException lastError = null;
        for (String candidate : candidates) {
            try {
                return callGemini(candidate, requestBody);
            } catch (WebClientResponseException error) {
                lastError = error;
                logger.warn("O modelo Gemini '{}' recusou a requisição (HTTP {}). Tentando o próximo modelo.", candidate, error.getStatusCode().value());
            }
        }

        // A lista da conta pode conter um modelo diferente dos padrões. Só o
        // adicionamos depois das tentativas explícitas e nunca repetimos um
        // modelo já testado.
        String discovered = resolveAvailableModel();
        if (discovered != null && candidates.add(discovered)) {
            try {
                return callGemini(discovered, requestBody);
            } catch (WebClientResponseException error) {
                lastError = error;
            }
        }
        if (lastError != null) throw lastError;
        throw new IllegalStateException("Nenhum modelo Gemini disponível para esta chave.");
    }

    private void addModel(Set<String> candidates, String value) {
        if (value != null && !value.isBlank()) candidates.add(value.replaceFirst("^models/", ""));
    }

    private String resolveAvailableModel() throws Exception {
        String response = webClient.get()
                .uri("https://generativelanguage.googleapis.com/v1beta/models")
                .header("x-goog-api-key", apiKey)
                .retrieve()
                .bodyToMono(String.class)
                .block();
        JsonNode models = objectMapper.readTree(response).path("models");
        for (String preferredName : Arrays.asList("gemini-3.8-flash", "gemini-2.0-flash-001", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b", "gemini-2.5-flash-lite")) {
            for (JsonNode item : models) {
                String name = item.path("name").asText("").replaceFirst("^models/", "");
                if (preferredName.equals(name) && supportsGeneration(item)) return name;
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

    private String callGemini(String selectedModel, Map<String, Object> requestBody) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + selectedModel + ":generateContent";
        return webClient.post()
                .uri(url)
                .header("x-goog-api-key", apiKey)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}
