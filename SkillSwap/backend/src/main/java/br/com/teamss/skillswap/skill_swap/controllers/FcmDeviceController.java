package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Registers push tokens without exposing them in URLs or browser storage. */
@RestController
@RequestMapping("/api/devices")
public class FcmDeviceController {
    private final UserServiceDTO userServiceDTO;
    private final UserRepository userRepository;

    public FcmDeviceController(UserServiceDTO userServiceDTO, UserRepository userRepository) {
        this.userServiceDTO = userServiceDTO;
        this.userRepository = userRepository;
    }

    @PostMapping("/fcm-token")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody FcmTokenRequest request) {
        UserDTO current = userServiceDTO.getAuthenticatedUser();
        User user = userRepository.findById(current.getUserId()).orElseThrow();
        user.setFcmToken(request.token().trim());
        userRepository.save(user);
        return ResponseEntity.ok(Map.of("registered", true));
    }

    @DeleteMapping("/fcm-token")
    public ResponseEntity<Void> revoke() {
        UserDTO current = userServiceDTO.getAuthenticatedUser();
        User user = userRepository.findById(current.getUserId()).orElseThrow();
        user.setFcmToken(null);
        userRepository.save(user);
        return ResponseEntity.noContent().build();
    }

    public record FcmTokenRequest(
        @NotBlank @Size(max = 4096) String token
    ) {}
}
