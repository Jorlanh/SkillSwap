package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.GenerateAchievementImageRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserAchievementDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Achievement;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.entities.UserAchievement;
import br.com.teamss.skillswap.skill_swap.model.repositories.AchievementRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProposalRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserAchievementRepository;
import br.com.teamss.skillswap.skill_swap.model.services.AchievementService;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.SwapCoinWalletService;
import br.com.teamss.skillswap.skill_swap.util.ByteArrayMultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AchievementServiceImpl implements AchievementService {

    private final AchievementRepository achievementRepository;
    private final UserAchievementRepository userAchievementRepository;
    private final ProposalRepository proposalRepository;
    private final UserServiceDTO userServiceDTO;
    private final FileUploadService fileUploadService;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final SwapCoinWalletService walletService;

    @Value("${gemini.api.key}")
    private String apiKey;

    public AchievementServiceImpl(AchievementRepository achievementRepository, UserAchievementRepository userAchievementRepository,
                                  ProposalRepository proposalRepository, UserServiceDTO userServiceDTO, FileUploadService fileUploadService,
                                  WebClient.Builder webClientBuilder, SwapCoinWalletService walletService) {
        this.achievementRepository = achievementRepository;
        this.userAchievementRepository = userAchievementRepository;
        this.proposalRepository = proposalRepository;
        this.userServiceDTO = userServiceDTO;
        this.fileUploadService = fileUploadService;
        this.webClient = webClientBuilder.build();
        this.objectMapper = new ObjectMapper();
        this.walletService = walletService;
    }

    @Override
    public void checkAndUnlockAchievements(User user) {
        checkTradeAchievements(user, "TRADES_10", 10);
    }

    private void checkTradeAchievements(User user, String achievementKey, int requiredTrades) {
        achievementRepository.findByAchievementKey(achievementKey).ifPresent(achievement -> {
            boolean alreadyUnlocked = userAchievementRepository.existsByUser_UserIdAndAchievement_Id(user.getUserId(), achievement.getId());
            if (!alreadyUnlocked) {
                long completedTrades = proposalRepository.countByStatusAndParticipant("COMPLETED", user.getUserId());
                if (completedTrades >= requiredTrades) {
                    UserAchievement userAchievement = new UserAchievement();
                    userAchievement.setUser(user);
                    userAchievement.setAchievement(achievement);
                    userAchievement.setImageUrl("PENDING_GENERATION");
                    userAchievementRepository.save(userAchievement);
                    long reward = "TRADES_10".equals(achievementKey) ? 25 : 5;
                    walletService.grantAchievementReward(user.getUserId(), reward,
                        "Recompensa da conquista " + achievementKey,
                        "achievement:" + user.getUserId() + ":" + achievementKey);
                }
            }
        });
    }

    @Override
    public UserAchievement generateAchievementImage(GenerateAchievementImageRequestDTO request) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        UserAchievement userAchievement = userAchievementRepository.findById(request.getAchievementId())
                .orElseThrow(() -> new IllegalStateException("Conquista de utilizador não encontrada."));

        if (!userAchievement.getUser().getUserId().equals(authenticatedUser.getUserId())) {
            throw new AccessDeniedException("Você não tem permissão para gerar a imagem para esta conquista.");
        }

        if (!"PENDING_GENERATION".equals(userAchievement.getImageUrl())) {
            throw new IllegalStateException("A imagem para esta conquista já foi gerada.");
        }

        String achievementName = userAchievement.getAchievement().getName();
        String finalPrompt = String.format(
            "%s. O nome da conquista é '%s'. Inclua este nome de forma visível na parte inferior da imagem.",
            request.getUserPrompt(), achievementName
        );

        try {
            byte[] imageBytes = generateImageWithGemini(finalPrompt);
            String filename = "achievement_" + userAchievement.getId() + ".png";
            MultipartFile imageFile = new ByteArrayMultipartFile(imageBytes, "file", filename, "image/png");
            String imageUrl = fileUploadService.uploadFile(imageFile);
            
            if (imageUrl == null || imageUrl.isBlank()) {
                throw new RuntimeException("A URL da imagem retornada pelo serviço de upload está vazia.");
            }

            userAchievement.setImageUrl(imageUrl);
            userAchievement.setCustomPrompt(request.getUserPrompt());
            return userAchievementRepository.save(userAchievement);

        } catch (Exception e) {
            throw new RuntimeException("Falha ao gerar ou fazer o upload da imagem da conquista: " + e.getMessage(), e);
        }
    }

    private byte[] generateImageWithGemini(String prompt) throws IOException {
        // URL da API oficial do Google AI Studio (Imagem) usando Imagen 3 ou similar disponível via REST
        // Nota: Substitua "imagen-3.0-generate-001" pelo modelo correto de imagem da API REST caso a Google atualize a nomenclatura.
        String url = "https://generativelanguage.googleapis.com/v1beta/models/imagen-3.0-generate-001:predict";

        // Montagem do Payload de requisição (JSON) esperado pela API REST do Google AI Studio
        Map<String, Object> requestBody = Map.of(
            "instances", List.of(
                Map.of("prompt", "Gere uma imagem com a seguinte descrição: " + prompt)
            ),
            "parameters", Map.of(
                "sampleCount", 1
            )
        );

        try {
            String responseJson = webClient.post()
                    .uri(url)
                    .header("x-goog-api-key", apiKey)
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(responseJson);
            JsonNode predictionsNode = rootNode.path("predictions");
            
            if (predictionsNode.isMissingNode() || !predictionsNode.isArray() || predictionsNode.isEmpty()) {
                throw new IOException("A API Gemini não retornou dados de imagem válidos. Verifique as quotas e os logs.");
            }

            // O Google retorna a imagem em Base64 dentro do node de predictions
            String base64Image = predictionsNode.get(0).path("bytesBase64Encoded").asText();
            
            if (base64Image == null || base64Image.isEmpty()) {
                throw new IOException("A imagem retornada veio vazia.");
            }

            return Base64.getDecoder().decode(base64Image);

        } catch (Exception e) {
            throw new IOException("Erro na comunicação com a API do Google AI Studio: " + e.getMessage(), e);
        }
    }

    @Override
    public List<UserAchievementDTO> getUserAchievements(UUID userId) {
        return userAchievementRepository.findByUser_UserId(userId).stream()
                .map(ua -> new UserAchievementDTO(
                        ua.getAchievement().getName(),
                        ua.getAchievement().getDescription(),
                        ua.getImageUrl(),
                        ua.getUnlockedAt()))
                .collect(Collectors.toList());
    }
}
