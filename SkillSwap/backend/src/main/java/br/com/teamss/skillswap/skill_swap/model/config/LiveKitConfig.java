package br.com.teamss.skillswap.skill_swap.model.config;

import io.livekit.server.RoomServiceClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiveKitConfig {

    @Value("${livekit.api.url}")
    private String livekitUrl; 

    @Value("${livekit.api.key}")
    private String livekitApiKey; 

    @Value("${livekit.api.secret}")
    private String livekitApiSecret; 

    @Bean
    public RoomServiceClient roomServiceClient() {
        // Inicialização estática para o SDK Java do LiveKit
        return RoomServiceClient.createClient(livekitUrl, livekitApiKey, livekitApiSecret);
    }
}