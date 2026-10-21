package br.com.teamss.skillswap.skill_swap.model.config;

import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenSearchConfig {

    private static final Logger logger = LoggerFactory.getLogger(OpenSearchConfig.class);

    // Removidos os valores padrão (:localhost, :9200, etc) para evitar exposição
    @Value("${opensearch.host}")
    private String host;

    @Value("${opensearch.port}")
    private int port;

    @Value("${opensearch.username}")
    private String username;

    @Value("${opensearch.password}")
    private String password;

    @Bean
    public OpenSearchClient openSearchClient() {
        logger.info("Iniciando configuração do OpenSearchClient para o host: {}", host);
        try {
            final BasicCredentialsProvider credentialsProvider = new BasicCredentialsProvider();
            credentialsProvider.setCredentials(
                new AuthScope(host, port),
                new UsernamePasswordCredentials(username, password)
            );

            // Constrói o RestClient de baixo nível
            // Em ambiente de produção, altere "http" para "https" conforme necessário
            final RestClient restClient = RestClient.builder(new HttpHost(host, port, "http")) 
                .setHttpClientConfigCallback(httpClientBuilder -> 
                    httpClientBuilder.setDefaultCredentialsProvider(credentialsProvider)
                )
                .build();

            // Constrói o Transport e o Cliente de alto nível
            final OpenSearchTransport transport = new RestClientTransport(
                restClient, 
                new JacksonJsonpMapper()
            );

            return new OpenSearchClient(transport);
        } catch (Exception e) {
            logger.error("Falha crítica ao criar o OpenSearchClient: {}", e.getMessage());
            throw new RuntimeException("Erro ao configurar conexão com OpenSearch", e);
        }
    }
}