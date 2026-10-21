package br.com.teamss.skillswap.skill_swap.model.config;

import br.com.teamss.skillswap.skill_swap.dto.SwapCoinWalletResponse;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/** Pushes an auditable wallet version to connected clients after every mutation. */
@Component
public class WalletBalanceWebSocketHandler extends TextWebSocketHandler {
    private final JwtDecoder jwtDecoder;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final Map<UUID, Map<String, WebSocketSession>> sessions = new ConcurrentHashMap<>();

    public WalletBalanceWebSocketHandler(JwtDecoder jwtDecoder, UserRepository userRepository, ObjectMapper objectMapper) {
        this.jwtDecoder = jwtDecoder;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String path = session.getUri() == null ? "" : session.getUri().getPath();
        UUID requestedUser = UUID.fromString(path.replaceAll(".*/wallet-stream/", ""));
        session.getAttributes().put("requestedUserId", requestedUser);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        if (session.getAttributes().get("userId") != null) return;
        try {
            var json = objectMapper.readTree(message.getPayload());
            if (!"auth".equals(json.path("type").asText())) throw new IllegalArgumentException("autenticação obrigatória");
            String token = json.path("token").asText(null);
            if (token == null || token.isBlank()) throw new IllegalArgumentException("token ausente");
            Jwt jwt = jwtDecoder.decode(token);
            String email = jwt.getClaimAsString("email");
            UUID localUser = userRepository.findByEmail(email).orElseThrow().getUserId();
            UUID requestedUser = (UUID) session.getAttributes().get("requestedUserId");
            if (!requestedUser.equals(localUser)) throw new IllegalArgumentException("usuário divergente");
            session.getAttributes().put("userId", localUser);
            sessions.computeIfAbsent(localUser, ignored -> new ConcurrentHashMap<>()).put(session.getId(), session);
            session.sendMessage(new TextMessage(objectMapper.writeValueAsString(Map.of(
                "type", "wallet.stream.ready", "userId", localUser.toString()))));
        } catch (Exception exception) {
            session.close(CloseStatus.POLICY_VIOLATION);
        }
    }

    public void broadcast(UUID userId, SwapCoinWalletResponse wallet) {
        Map<String, WebSocketSession> userSessions = sessions.get(userId);
        if (userSessions == null) return;
        try {
            String payload = objectMapper.writeValueAsString(Map.of(
                "type", "wallet.balance.updated", "balance", wallet.balance(), "version", wallet.version()));
            userSessions.values().removeIf(session -> {
                if (!session.isOpen()) return true;
                try { session.sendMessage(new TextMessage(payload)); return false; }
                catch (IOException error) { return true; }
            });
        } catch (IOException ignored) { }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Object userId = session.getAttributes().get("userId");
        if (userId instanceof UUID uuid) {
            Map<String, WebSocketSession> userSessions = sessions.get(uuid);
            if (userSessions != null) {
                userSessions.remove(session.getId());
                if (userSessions.isEmpty()) sessions.remove(uuid);
            }
        }
    }
}
