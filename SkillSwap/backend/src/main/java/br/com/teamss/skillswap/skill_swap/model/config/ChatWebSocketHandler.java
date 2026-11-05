package br.com.teamss.skillswap.skill_swap.model.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.dto.ChatMessageRequestDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import br.com.teamss.skillswap.skill_swap.model.services.ChatService;
import java.util.UUID;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private final RedisTemplate<String, Object> redisTemplate;
    private final ChannelTopic chatTopic;
    private final ObjectMapper objectMapper;
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final ChatService chatService;

    // Chaves do Redis para mapear usuário -> sessão
    private static final String REDIS_KEY_USER_TO_SESSION = "skillswap:chat:user_sessions";
    
    // Mapeia o ID da sessão para a sessão (apenas sessões vivas NESTA instância)
    private final Map<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();
    // Mapeia o ID da sessão para o ID do usuário (apenas sessões vivas NESTA instância)
    private final Map<String, String> localSessionToUser = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(RedisTemplate<String, Object> redisTemplate,
                                ChannelTopic chatTopic,
                                ObjectMapper objectMapper,
                                JwtDecoder jwtDecoder,
                                UserRepository userRepository,
                                ChatService chatService) {
        this.redisTemplate = redisTemplate;
        this.chatTopic = chatTopic;
        this.objectMapper = objectMapper;
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
        this.chatService = chatService;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        localSessions.put(session.getId(), session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode jsonMessage = objectMapper.readTree(message.getPayload());
        String type = jsonMessage.has("type") ? jsonMessage.get("type").asText() : null;

        if ("auth".equals(type)) {
            try {
                String token = jsonMessage.path("token").asText(null);
                if (token == null || token.isBlank()) throw new IllegalArgumentException("token ausente");
                Jwt jwt = jwtDecoder.decode(token);
                String email = jwt.getClaimAsString("email");
                User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("usuário não encontrado"));
                String userId = user.getUserId().toString();
                localSessionToUser.put(session.getId(), userId);
                session.getAttributes().put("userId", userId);
                redisTemplate.opsForHash().put(REDIS_KEY_USER_TO_SESSION, userId, session.getId());
                session.sendMessage(new TextMessage("{\"type\":\"auth.ok\"}"));
            } catch (Exception exception) {
                session.close(CloseStatus.POLICY_VIOLATION);
            }
            return;
        }

        String authenticatedUserId = localSessionToUser.get(session.getId());
        if (authenticatedUserId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (jsonMessage.has("senderId") && !authenticatedUserId.equals(jsonMessage.get("senderId").asText())) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        if (jsonMessage.isObject()) {
            ((com.fasterxml.jackson.databind.node.ObjectNode) jsonMessage).put("senderId", authenticatedUserId);
        }

        if (!jsonMessage.hasNonNull("receiverId") || !jsonMessage.hasNonNull("content")) {
            session.sendMessage(new TextMessage("{\"type\":\"error\",\"message\":\"Mensagem inválida.\"}"));
            return;
        }

        try {
            com.fasterxml.jackson.databind.node.ObjectNode requestNode =
                    ((com.fasterxml.jackson.databind.node.ObjectNode) jsonMessage).deepCopy();
            requestNode.remove("type");
            ChatMessageRequestDTO request = objectMapper.treeToValue(requestNode, ChatMessageRequestDTO.class);
            request.setSenderId(UUID.fromString(authenticatedUserId));
            ChatMessage saved = chatService.sendMessage(request);

            // Persiste pelo mesmo serviço usado pelo endpoint HTTP e publica
            // somente um payload enxuto, evitando serializar User e criar ciclos.
            var persistedPayload = objectMapper.createObjectNode()
                    .put("id", saved.getMessageId())
                    .put("senderId", saved.getSender().getUserId().toString())
                    .put("receiverId", saved.getReceiver().getUserId().toString())
                    .put("content", saved.getContent() == null ? "" : saved.getContent())
                    .put("sentAt", saved.getSentAt().toString())
                    .put("type", "TEXT");
            redisTemplate.convertAndSend(chatTopic.getTopic(), persistedPayload.toString());
        } catch (Exception exception) {
            session.sendMessage(new TextMessage(objectMapper.createObjectNode()
                    .put("type", "error")
                    .put("message", exception.getMessage() == null ? "Não foi possível enviar a mensagem." : exception.getMessage())
                    .toString()));
        }
    }

    /**
     * Este método é chamado pelo RedisMessageSubscriber quando uma mensagem
     * chega em *qualquer* servidor.
     */
    public void broadcastMessageToLocalSessions(String messagePayload) {
        try {
            JsonNode message = objectMapper.readTree(messagePayload);
            String receiverId = message.has("receiverId") ? message.get("receiverId").asText() : null;
            
            if (receiverId == null) return; // Não é uma mensagem de chat direcionada

            // Verifica no Redis qual sessão pertence a este usuário
            String targetSessionId = (String) redisTemplate.opsForHash().get(REDIS_KEY_USER_TO_SESSION, receiverId);

            if (targetSessionId != null && localSessions.containsKey(targetSessionId)) {
                // A sessão do destinatário está NESTA instância do servidor
                WebSocketSession clientSession = localSessions.get(targetSessionId);
                if (clientSession != null && clientSession.isOpen()) {
                    clientSession.sendMessage(new TextMessage(messagePayload));
                }
            }
        } catch (Exception e) {
            System.err.println("Falha ao processar broadcast do Chat via Redis: " + e.getMessage());
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        localSessions.remove(sessionId); 

        // Remove do Redis
        String userId = localSessionToUser.remove(sessionId);
        if (userId != null) {
            try {
                // Remove apenas se o ID da sessão for o mesmo (evita condição de corrida)
                if (sessionId.equals(redisTemplate.opsForHash().get(REDIS_KEY_USER_TO_SESSION, userId))) {
                    redisTemplate.opsForHash().delete(REDIS_KEY_USER_TO_SESSION, userId);
                }
            } catch (Exception e) {
                 System.err.println("Erro ao limpar sessão de chat do Redis: " + sessionId);
            }
        }
        System.out.println("Sessão de Chat fechada: " + sessionId);
    }
}
