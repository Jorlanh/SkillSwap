package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import org.springframework.security.oauth2.jwt.Jwt;
import java.util.Optional;

public interface LocalUserService {
    User findOrCreateUser(Jwt jwt);
    Optional<User> findByEmail(String email);
}