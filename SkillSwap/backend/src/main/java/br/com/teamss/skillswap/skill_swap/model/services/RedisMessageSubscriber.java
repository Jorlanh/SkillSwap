package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.config.ChatWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisMessageSubscriber implements MessageListener {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private ChatWebSocketHandler webSocketHandler; // Injeta o handler de CHAT

    // Este método é chamado pelo RedisConfig quando uma mensagem chega
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            // Desserializa a mensagem
            String messagePayload = redisTemplate.getStringSerializer().deserialize(message.getBody());
            
            // Encaminha a mensagem para o handler local de WebSocket
            webSocketHandler.broadcastMessageToLocalSessions(messagePayload);

        } catch (Exception e) {
            System.err.println("Erro ao processar mensagem do Redis Pub/Sub: " + e.getMessage());
        }
    }
}