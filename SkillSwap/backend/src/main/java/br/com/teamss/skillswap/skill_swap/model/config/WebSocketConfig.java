package br.com.teamss.skillswap.skill_swap.model.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;
    
    @Autowired
    private ChatWebSocketHandler chatWebSocketHandler; // Injeta o novo handler de CHAT

    @Autowired
    private WalletBalanceWebSocketHandler walletBalanceWebSocketHandler;

    // O bean ConcurrentHashMap e o VideoCallHandler foram removidos.
    // O ChatWebSocketHandler agora é um @Component gerenciado pelo Spring.

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Registra o handler de chat, que agora é stateless (usa Redis)
        registry.addHandler(chatWebSocketHandler, "/chat") // Endpoint para chat
                .setAllowedOrigins(allowedOrigins);
        registry.addHandler(walletBalanceWebSocketHandler, "/wallet-stream/{userId}")
                .setAllowedOrigins(allowedOrigins);
    }
}
