package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.AccessibilityPreferencesRequest;
import br.com.teamss.skillswap.skill_swap.dto.AccessibilityPreferencesResponse;
import br.com.teamss.skillswap.skill_swap.model.entities.AccessibilityPreferences;
import br.com.teamss.skillswap.skill_swap.model.repositories.AccessibilityPreferencesRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/accessibility/preferences")
@PreAuthorize("isAuthenticated()")
public class AccessibilityPreferencesController {
    private final AccessibilityPreferencesRepository preferencesRepository;
    private final UserRepository userRepository;
    private final UserServiceDTO userServiceDTO;

    public AccessibilityPreferencesController(
        AccessibilityPreferencesRepository preferencesRepository,
        UserRepository userRepository,
        UserServiceDTO userServiceDTO
    ) {
        this.preferencesRepository = preferencesRepository;
        this.userRepository = userRepository;
        this.userServiceDTO = userServiceDTO;
    }

    @GetMapping
    public ResponseEntity<AccessibilityPreferencesResponse> get() {
        UUID userId = userServiceDTO.getAuthenticatedUser().getUserId();
        AccessibilityPreferences preferences = preferencesRepository.findById(userId)
            .orElseGet(() -> defaults(userId));
        return ResponseEntity.ok(AccessibilityPreferencesResponse.from(preferences));
    }

    @PutMapping
    public ResponseEntity<AccessibilityPreferencesResponse> save(
        @Valid @RequestBody AccessibilityPreferencesRequest request
    ) {
        UUID userId = userServiceDTO.getAuthenticatedUser().getUserId();
        AccessibilityPreferences preferences = preferencesRepository.findById(userId)
            .orElseGet(() -> defaults(userId));

        preferences.setContrastMode(request.contrastMode());
        preferences.setFontScale(request.fontScale());
        preferences.setPreferredChannel(request.preferredChannel());
        preferences.setCaptionsEnabled(request.captionsEnabled());
        preferences.setImageDescriptionsEnabled(request.imageDescriptionsEnabled());
        preferences.setSimplifiedLanguageEnabled(request.simplifiedLanguageEnabled());
        preferences.setLibrasEnabled(request.librasEnabled());
        preferences.setKeyboardNavigationEnabled(request.keyboardNavigationEnabled());
        preferences.setVisualMarker(request.visualMarker());
        preferences.setAccessibilitySymbol(request.accessibilitySymbol());
        preferences.setUpdatedAt(Instant.now());

        return ResponseEntity.ok(AccessibilityPreferencesResponse.from(preferencesRepository.save(preferences)));
    }

    private AccessibilityPreferences defaults(UUID userId) {
        AccessibilityPreferences preferences = new AccessibilityPreferences();
        preferences.setUserId(userId);
        preferences.setUser(userRepository.findById(userId)
            .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado.")));
        return preferences;
    }
}
