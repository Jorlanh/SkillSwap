package br.com.teamss.skillswap.skill_swap.model.config;

import java.util.Arrays;
import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter; 
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import br.com.teamss.skillswap.skill_swap.filters.BanCheckFilter;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity // Permite usar @PreAuthorize("hasAuthority('VERIFIED')")
public class SecurityFilterChainConfig {

    @Value("${cors.allowed-origins}")
    private String[] allowedOrigins;

    private final BanCheckFilter banCheckFilter; 

    public SecurityFilterChainConfig(BanCheckFilter banCheckFilter) {
        this.banCheckFilter = banCheckFilter;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CONVERSOR JWT CUSTOMIZADO
     * Lê as permissões e o status de verificação de e-mail do Firebase
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        // O Firebase usa 'permissions' para roles customizadas
        authoritiesConverter.setAuthoritiesClaimName("permissions"); 
        authoritiesConverter.setAuthorityPrefix("ROLE_"); 

        JwtAuthenticationConverter jwtConverter = new JwtAuthenticationConverter();
        jwtConverter.setJwtGrantedAuthoritiesConverter(jwt -> {
            var authorities = new ArrayList<>(authoritiesConverter.convert(jwt));
            
            // Lógica do 2FA Gratuito: Se o e-mail estiver verificado, adiciona autoridade 'VERIFIED'
            Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");
            if (Boolean.TRUE.equals(emailVerified)) {
                authorities.add(() -> "VERIFIED");
            }
            
            return authorities;
        });
        return jwtConverter;
    }

    @Bean
    @Order(1)
    @Profile("dev")
    public SecurityFilterChain h2ConsoleSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .securityMatcher(new AntPathRequestMatcher("/h2-console/**"))
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .csrf(csrf -> csrf.disable())
            .headers(headers -> headers.frameOptions(f -> f.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain defaultSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(withDefaults())
            .csrf(csrf -> csrf.disable())
            
            // Gerenciamento de sessão: STATELESS para JWT (Firebase)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            .authorizeHttpRequests(auth -> auth
                // Endpoints Públicos
                // A Stripe assina cada webhook; nenhuma outra rota de pagamento é pública.
                .requestMatchers(HttpMethod.POST, "/api/payments/stripe/webhook").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/home/**", "/api/skills/**", "/api/rankings/**", "/api/search/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/privacy/policy", "/api/privacy/minor-policy", "/api/privacy/guardian-consent").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/privacy/guardian-consent/confirm").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/roles/**", "/api/profiles/user/{username}").permitAll()
                // O handshake WebSocket não carrega o JWT na URL. A autenticação
                // acontece como primeira mensagem, dentro dos handlers, e o
                // endpoint HTTP precisa aceitar essa etapa inicial.
                .requestMatchers("/chat/**", "/wallet-stream/**").permitAll()
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/error").permitAll()
                
                // Exigência de autenticação para o restante
                .anyRequest().authenticated()
            )
                        
            // CONFIGURAÇÃO RESOURCE SERVER: Valida o Token JWT contra as chaves públicas do Google
            .oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
            )
            
            // Filtro de Banimento após a autenticação
            .addFilterAfter(banCheckFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(Arrays.asList("Authorization", "Cache-Control", "Content-Type", "Accept", "Origin", "X-Requested-With"));
        config.setExposedHeaders(Arrays.asList("Authorization", "X-Rate-Limit-Remaining", "Access-Control-Allow-Origin"));
        // A API usa Bearer tokens, não cookies. Evita CORS credentialed desnecessário.
        config.setAllowCredentials(false);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
