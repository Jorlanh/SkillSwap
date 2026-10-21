package br.com.teamss.skillswap.skill_swap.controllers;

import java.util.Map;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.PrivacyConsentService;
import br.com.teamss.skillswap.skill_swap.model.repositories.LessonRepository;
import br.com.teamss.skillswap.skill_swap.model.entities.Lesson;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final UserServiceDTO userServiceDTO;
    private final RoomServiceClient roomServiceClient;
    private final PrivacyConsentService privacyConsentService;
    private final LessonRepository lessonRepository;

    @Value("${livekit.join-window-before-minutes:30}")
    private long joinWindowBeforeMinutes;

    @Value("${livekit.join-window-after-minutes:180}")
    private long joinWindowAfterMinutes;

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    public VideoController(UserServiceDTO userServiceDTO, RoomServiceClient roomServiceClient,
                           PrivacyConsentService privacyConsentService, LessonRepository lessonRepository) {
        this.userServiceDTO = userServiceDTO;
        this.roomServiceClient = roomServiceClient;
        this.privacyConsentService = privacyConsentService;
        this.lessonRepository = lessonRepository;
    }

    /**
     * Endpoint para gerar o token de acesso ao LiveKit.
     * O cliente React chama este endpoint ao entrar em uma sala.
     */
    @PostMapping("/join-room")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getJoinToken(@RequestBody Map<String, String> request) {
        String roomName = request.get("roomName");
        if (roomName == null || roomName.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "roomName é obrigatório."));
        }

        // Pega o usuário autenticado via UserServiceDTO
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();

        if (!privacyConsentService.isGranted(authenticatedUser.getUserId(), "OPTIONAL_VIDEO")) {
            return ResponseEntity.status(428).body(Map.of("error", "Ative o consentimento opcional de vídeo no Centro de Privacidade."));
        }

        if (!roomName.startsWith("lesson-")) {
            return ResponseEntity.status(403).body(Map.of("error", "A sala precisa estar vinculada a uma aula."));
        }
        Lesson lesson;
        try {
            Long lessonId = Long.parseLong(roomName.substring("lesson-".length()));
            lesson = lessonRepository.findById(lessonId).orElseThrow();
            boolean participant = lesson.getTeacher() != null && authenticatedUser.getUserId().equals(lesson.getTeacher().getUserId())
                || lesson.getStudent() != null && authenticatedUser.getUserId().equals(lesson.getStudent().getUserId());
            if (!participant) return ResponseEntity.status(403).body(Map.of("error", "Você não participa desta aula."));
            if ("CANCELLED".equalsIgnoreCase(lesson.getStatus())
                || "CANCELED".equalsIgnoreCase(lesson.getStatus())
                || "REJECTED".equalsIgnoreCase(lesson.getStatus())) {
                return ResponseEntity.status(403).body(Map.of("error", "A videochamada não está disponível para uma aula cancelada."));
            }
            if (lesson.getScheduledTime() == null) {
                return ResponseEntity.status(403).body(Map.of("error", "A aula não possui horário para autorizar a sala."));
            }
            Instant now = Instant.now();
            Instant earliest = lesson.getScheduledTime().minus(Duration.ofMinutes(joinWindowBeforeMinutes));
            Instant latest = lesson.getScheduledTime().plus(Duration.ofMinutes(joinWindowAfterMinutes));
            if (now.isBefore(earliest) || now.isAfter(latest)) {
                return ResponseEntity.status(403).body(Map.of("error", "A sala só pode ser acessada no período próximo ao horário da aula."));
            }
        } catch (Exception exception) {
            return ResponseEntity.status(403).body(Map.of("error", "A sala da aula é inválida."));
        }

        try {
            // Criação do Token. Parâmetros TTL podem ser adicionados isoladamente, se necessário.
            // O padrão do SDK para expiração é de 6 horas caso não seja declarado.
            AccessToken token = new AccessToken(livekitApiKey, livekitApiSecret);
            
            // Setters executados de forma sequencial, já que retornam tipo primitivo 'void'
            token.setIdentity(authenticatedUser.getUserId().toString());
            token.setName(authenticatedUser.getUsername());
            
            // Definição das permissões.
            // Nota técnica: O objeto RoomJoin(true) concede, por default na arquitetura do LiveKit,
            // as permissões de publicar e escutar (canPublish, canSubscribe) para a sala em questão.
            token.addGrants(new RoomJoin(true), new RoomName(roomName));

            // Gera a assinatura criptográfica JWT
            String jwt = token.toJwt();

            // Retorna o payload de autorização
            return ResponseEntity.ok(Map.of(
                "token", jwt,
                "lessonId", lesson.getLessonId(),
                "safetyNotice", "A sala é exclusiva para os participantes autorizados da aula e não é gravada pelo SkillSwap."
            ));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Falha ao gerar token: " + e.getMessage()
            ));
        }
    }
}
