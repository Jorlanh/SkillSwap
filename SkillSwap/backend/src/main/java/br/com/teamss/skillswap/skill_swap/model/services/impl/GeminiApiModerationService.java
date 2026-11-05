package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
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
public class GeminiApiModerationService implements ContentModerationService {

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model:gemini-3.8-flash}")
    private String model;

    @Value("${gemini.api.fallback-model:gemini-2.0-flash-001}")
    private String fallbackModel;

    private final WebClient webClient;
    private final ObjectMapper objectMapper;

    public GeminiApiModerationService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public boolean isContentInappropriate(String text) {
        if (apiKey == null || apiKey.isBlank() || text == null || text.isBlank()) {
            System.err.println("AVISO: Credenciais da API Gemini não configuradas ou texto vazio. Moderação desativada.");
            return false;
        }

        try {
            String prompt = String.format(
                "Você é um moderador de conteúdo para uma plataforma social chamada SkillSwap. " +
                "Sua tarefa é analisar o texto a seguir e determinar se ele viola alguma das seguintes categorias: " +
                "racismo, xenofobia, homofobia, transfobia, discriminação contra deficiência, perseguição, assédio, " +
                "ameaça real, incentivo à violência, conteúdo sexual envolvendo menores, exposição de dados pessoais, " +
                "spam abusivo ou incentivo à automutilação. " +
                "Marque SIM somente quando houver ataque ou ameaça contra uma pessoa ou grupo, incentivo a crime, " +
                "ameaça concreta ou assédio abusivo. Não marque críticas, opiniões políticas, debates, linguagem coloquial " +
                "ou discordância respeitosa apenas por serem controversos. Responda apenas com SIM ou NAO. " +
                "Texto para analisar: \"%s\"", text
            );

            Map<String, Object> requestBody = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );

            String responseJson = callGeminiWithFallback(requestBody);

            JsonNode rootNode = objectMapper.readTree(responseJson);
            String modelResponse = rootNode.path("candidates").get(0).path("content").path("parts").get(0).path("text").asText().trim();

            return modelResponse.toUpperCase().startsWith("SIM");

        } catch (Exception e) {
            System.err.println("Erro ao comunicar com a API do Google AI Studio (Gemini): " + e.getMessage());
            return false;
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
        addModel(candidates, "gemini-2.0-flash-001");
        WebClientResponseException lastError = null;
        for (String candidate : candidates) {
            try {
                return callGemini(candidate, requestBody);
            } catch (WebClientResponseException error) {
                lastError = error;
            }
        }
        String discovered = resolveAvailableModel();
        if (discovered != null && candidates.add(discovered)) return callGemini(discovered, requestBody);
        if (lastError != null) throw lastError;
        throw new IllegalStateException("Nenhum modelo Gemini disponível para moderação.");
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
        for (String preferred : Arrays.asList("gemini-3.8-flash", "gemini-2.0-flash-001", "gemini-2.0-flash", "gemini-1.5-flash", "gemini-1.5-flash-8b", fallbackModel)) {
            for (JsonNode item : models) {
                String name = item.path("name").asText("").replaceFirst("^models/", "");
                boolean supports = false;
                for (JsonNode method : item.path("supportedGenerationMethods")) supports |= "generateContent".equalsIgnoreCase(method.asText());
                if (preferred.equals(name) && supports) return name;
            }
        }
        throw new IllegalStateException("Nenhum modelo Gemini disponível para moderação.");
    }
}
