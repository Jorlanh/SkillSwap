package br.com.teamss.skillswap.skill_swap.controllers;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ProposalService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import io.livekit.server.AccessToken;
import io.livekit.server.RoomJoin;
import io.livekit.server.RoomName;
import io.livekit.server.RoomServiceClient;
import java.util.UUID;

@RestController
@RequestMapping("/api/video")
public class VideoController {

    private final UserServiceDTO userServiceDTO;
    private final RoomServiceClient roomServiceClient;
    private final ProposalService proposalService;
    private final NotificationService notificationService;

    @Value("${livekit.api.key}")
    private String livekitApiKey;

    @Value("${livekit.api.secret}")
    private String livekitApiSecret;

    public VideoController(UserServiceDTO userServiceDTO, RoomServiceClient roomServiceClient, ProposalService proposalService,
                           NotificationService notificationService) {
        this.userServiceDTO = userServiceDTO;
        this.roomServiceClient = roomServiceClient;
        this.proposalService = proposalService;
        this.notificationService = notificationService;
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

        String peerUserId = request.get("peerUserId");
        if (peerUserId == null || peerUserId.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "peerUserId é obrigatório."));
        }

        // Pega o usuário autenticado via UserServiceDTO
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        try {
            if (!proposalService.canMessage(authenticatedUser.getUserId(), UUID.fromString(peerUserId))) {
                return ResponseEntity.status(403).body(Map.of("error", "A chamada só fica disponível depois que uma proposta é aceita."));
            }
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "peerUserId inválido."));
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

            // Registra a tentativa de entrada para que o outro participante saiba que a chamada foi iniciada.
            // A autorização continua dependendo de uma proposta aceita; a notificação não concede acesso.
            try {
                notificationService.createNotification(
                        UUID.fromString(peerUserId),
                        authenticatedUser.getUsername() + " iniciou uma chamada com você. Acesse as mensagens para entrar."
                );
            } catch (RuntimeException notificationError) {
                // Uma indisponibilidade da fila de notificações não deve impedir a entrada na sala.
                System.err.println("Não foi possível registrar a notificação da chamada: " + notificationError.getMessage());
            }

            // Retorna o payload de autorização
            return ResponseEntity.ok(Map.of("token", jwt));

        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "error", "Falha ao gerar token: " + e.getMessage()
            ));
        }
    }
}
