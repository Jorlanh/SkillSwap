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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ChatbotServiceImpl implements ChatbotService {

    private static final Logger logger = LoggerFactory.getLogger(ChatbotServiceImpl.class);
    private final TranslationService translationService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-2.5-flash}")
    private String model;

    public ChatbotServiceImpl(TranslationService translationService, WebClient.Builder webClientBuilder) {
        this.translationService = translationService;
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ChatbotResponseDTO getChatbotResponse(ChatbotRequestDTO requestDTO) {
        if (apiKey == null || apiKey.isBlank()) {
            logger.error("API Key do Gemini não configurada. Chatbot indisponível.");
            return new ChatbotResponseDTO("Desculpe, o chatbot está temporariamente indisponível.", null, false);
        }
        if (requestDTO == null || requestDTO.getMessage() == null || requestDTO.getMessage().isBlank()) {
             logger.warn("Recebida requisição de chatbot inválida (mensagem vazia).");
             return new ChatbotResponseDTO("Por favor, envie uma mensagem.", null, false);
        }

        try {
            String systemPrompt = "Você é um assistente prestativo da plataforma SkillSwap. Seu objetivo é ajudar os usuários a navegar na plataforma, encontrar informações sobre habilidades, entender como funcionam as trocas e responder perguntas gerais sobre o SkillSwap. Seja amigável, claro e conciso. Responda sempre em português brasileiro.";

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("system_instruction", Map.of("parts", List.of(Map.of("text", systemPrompt))));
            requestBody.put("contents", List.of(Map.of("parts", List.of(Map.of("text", requestDTO.getMessage())))));

            String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent";

            String responseJson = webClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode candidatesNode = rootNode.path("candidates");

            String rawResponse = "";
            if (!candidatesNode.isMissingNode() && candidatesNode.isArray() && !candidatesNode.isEmpty()) {
                 rawResponse = candidatesNode.get(0).path("content").path("parts").get(0).path("text").asText();
            }

            if (rawResponse.isBlank()) {
                logger.warn("Resposta do Gemini não contém texto utilizável.");
                return new ChatbotResponseDTO("Desculpe, não consegui gerar uma resposta neste momento.", null, false);
            }

            String finalResponse = rawResponse;
            boolean wasSimplified = false;

            if (requestDTO.isSimplifyResponse() && translationService != null) {
                finalResponse = translationService.simplifyText(rawResponse);
                wasSimplified = !finalResponse.equals(rawResponse);
            }

            return new ChatbotResponseDTO(finalResponse, null, wasSimplified);

        } catch (Exception e) {
            logger.error("Erro ao comunicar com a API do Google AI Studio: {}", e.getMessage(), e);
            return new ChatbotResponseDTO("Desculpe, ocorreu um erro inesperado. Tente novamente mais tarde.", null, false);
        }
    }
}
