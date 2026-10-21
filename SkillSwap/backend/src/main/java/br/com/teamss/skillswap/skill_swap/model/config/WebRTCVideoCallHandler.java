package br.com.teamss.skillswap.skill_swap.model.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class WebRTCVideoCallHandler extends TextWebSocketHandler {

    // Injeção dos componentes do Redis
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ChannelTopic webRtcTopic;

    // Chaves do Redis para armazenar o estado
    private static final String REDIS_KEY_SESSION_TO_ROOM = "skillswap:webrtc:session_to_room";
    private static final String REDIS_KEY_ROOM_PREFIX = "skillswap:webrtc:room:";

    // Este mapa é o ÚNICO estado local: armazena as sessões VIVAS *nesta* instância do servidor
    private final Map<String, WebSocketSession> localSessions = new ConcurrentHashMap<>();
    
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        localSessions.put(session.getId(), session);
        System.out.println("Sessão WebSocket local estabelecida: " + session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        JsonNode jsonMessage = objectMapper.readTree(message.getPayload());
        String type = jsonMessage.has("type") ? jsonMessage.get("type").asText() : null;

        if ("join_room".equals(type)) {
            handleJoinRoom(session, jsonMessage);
        }

        // Independentemente do tipo, publicamos a mensagem no Redis para que *todos* os servidores a recebam
        // Incluímos o ID da sessão de origem para evitar eco
        String messageWithSender = objectMapper.createObjectNode()
            .put("senderSessionId", session.getId())
            .set("payload", jsonMessage)
            .toString();

        redisTemplate.convertAndSend(webRtcTopic.getTopic(), messageWithSender);
    }

    /**
     * Este método é chamado pelo RedisMessageSubscriber quando uma mensagem
     * chega em *qualquer* servidor.
     */
    public void broadcastMessageToLocalSessions(String messagePayload) {
        try {
            JsonNode wrapper = objectMapper.readTree(messagePayload);
            String senderSessionId = wrapper.get("senderSessionId").asText();
            JsonNode originalMessage = wrapper.get("payload");
            String roomId = originalMessage.has("roomId") ? originalMessage.get("roomId").asText() : null;

            if (roomId == null) return;

            // Busca no Redis quais sessões estão nesta sala
            Map<Object, Object> sessionIdsInRoom = redisTemplate.opsForHash().entries(REDIS_KEY_ROOM_PREFIX + roomId);

            for (Object sessionIdObj : sessionIdsInRoom.keySet()) {
                String sessionId = (String) sessionIdObj;
                
                // Se a sessão NÃO for a que enviou E estiver conectada a *esta* instância do servidor...
                if (!sessionId.equals(senderSessionId) && localSessions.containsKey(sessionId)) {
                    WebSocketSession clientSession = localSessions.get(sessionId);
                    if (clientSession != null && clientSession.isOpen()) {
                        try {
                            // Envia a mensagem original
                            clientSession.sendMessage(new TextMessage(originalMessage.toString()));
                        } catch (IOException e) {
                            System.err.println("Falha ao enviar mensagem WS para sessão local: " + sessionId);
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Falha ao processar broadcast do Redis: " + e.getMessage());
        }
    }

    private void handleJoinRoom(WebSocketSession session, JsonNode jsonMessage) {
        String roomId = jsonMessage.get("roomId").asText();
        String sessionId = session.getId();

        // Armazena no Redis
        redisTemplate.opsForHash().put(REDIS_KEY_ROOM_PREFIX + roomId, sessionId, "true"); // Adiciona sessão à sala
        redisTemplate.opsForHash().put(REDIS_KEY_SESSION_TO_ROOM, sessionId, roomId); // Mapeia sessão para a sala

        System.out.println("Sessão " + sessionId + " entrou na sala " + roomId + " (registrado no Redis)");
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = session.getId();
        localSessions.remove(sessionId); // Remove da instância local

        // Remove do Redis
        try {
            String roomId = (String) redisTemplate.opsForHash().get(REDIS_KEY_SESSION_TO_ROOM, sessionId);
            if (roomId != null) {
                redisTemplate.opsForHash().delete(REDIS_KEY_ROOM_PREFIX + roomId, sessionId);
            }
            redisTemplate.opsForHash().delete(REDIS_KEY_SESSION_TO_ROOM, sessionId);
            
            System.out.println("Sessão WebSocket fechada: " + sessionId + ". Removido do Redis (Sala: " + roomId + ")");
        } catch (Exception e) {
             System.err.println("Erro ao limpar sessão do Redis: " + sessionId);
        }
    }
}