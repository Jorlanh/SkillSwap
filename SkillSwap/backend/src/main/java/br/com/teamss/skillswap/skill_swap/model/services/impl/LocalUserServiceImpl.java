package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.RoleRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.LocalUserService;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** JIT-provisions a Firebase identity and synchronizes its local roles. */
@Service
public class LocalUserServiceImpl implements LocalUserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String bootstrapAdminEmail;

    public LocalUserServiceImpl(
        UserRepository userRepository,
        RoleRepository roleRepository,
        PasswordEncoder passwordEncoder,
        @Value("${security.bootstrap-admin-email:}") String bootstrapAdminEmail
    ) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.bootstrapAdminEmail = bootstrapAdminEmail;
    }

    @Override
    @Transactional
    public User findOrCreateUser(Jwt jwt) {
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("O token não contém e-mail.");
        }

        User user = userRepository.findByEmail(email).orElseGet(() -> createUserFromJwt(jwt, email));
        boolean changed = ensureRole(user, "USER");
        if (bootstrapAdminEmail != null && !bootstrapAdminEmail.isBlank()
            && bootstrapAdminEmail.equalsIgnoreCase(email)) {
            changed |= ensureRole(user, "ADMIN");
        }
        return changed ? userRepository.save(user) : user;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    private boolean ensureRole(User user, String roleName) {
        if (user.getRoles().stream().anyMatch(role -> roleName.equalsIgnoreCase(role.getName()))) return false;
        Role role = roleRepository.findByNameIgnoreCase(roleName)
            .orElseGet(() -> roleRepository.save(new Role(null, roleName)));
        user.getRoles().add(role);
        return true;
    }

    private User createUserFromJwt(Jwt jwt, String email) {
        User newUser = new User();
        newUser.setUserId(UUID.randomUUID());
        newUser.setEmail(email);

        String candidate = firstNonBlank(jwt.getClaimAsString("nickname"), jwt.getClaimAsString("name"), email.split("@")[0]);
        String username = candidate.toLowerCase().replaceAll("[^a-z0-9_]+", "_");
        if (username.isBlank() || userRepository.existsByUsername(username)) {
            username = email.substring(0, email.indexOf('@')).toLowerCase().replaceAll("[^a-z0-9_]+", "_")
                + "_" + UUID.randomUUID().toString().substring(0, 6);
        }
        newUser.setUsername(username);
        newUser.setName(firstNonBlank(jwt.getClaimAsString("name"), username));
        // The application never authenticates this value; Firebase remains the identity provider.
        newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
        newUser.setVerified(Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
        newUser.setVerifiedAt(newUser.getVerified() ? Instant.now() : null);
        newUser.setCreatedAt(Instant.now());
        ensureRole(newUser, "USER");
        return userRepository.save(newUser);
    }

    private String firstNonBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "skillswap_user";
    }
}
