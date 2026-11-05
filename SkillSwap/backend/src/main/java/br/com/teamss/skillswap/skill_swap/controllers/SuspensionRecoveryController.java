package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.SuspensionAppealRequest;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Ban;
import br.com.teamss.skillswap.skill_swap.model.repositories.BanRepository;
import br.com.teamss.skillswap.skill_swap.model.services.EmailService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/account/suspension")
public class SuspensionRecoveryController {

    private final UserServiceDTO userServiceDTO;
    private final BanRepository banRepository;
    private final EmailService emailService;
    private final String supportEmail;

    public SuspensionRecoveryController(
        UserServiceDTO userServiceDTO,
        BanRepository banRepository,
        EmailService emailService,
        @Value("${app.support.email:suporte.skillswap@gmail.com}") String supportEmail
    ) {
        this.userServiceDTO = userServiceDTO;
        this.banRepository = banRepository;
        this.emailService = emailService;
        this.supportEmail = supportEmail;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status() {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        List<Map<String, Object>> activeBans = banRepository
            .findByUser_UserIdAndActiveTrue(user.getUserId())
            .stream()
            .filter(this::isActive)
            .map(this::toResponse)
            .toList();

        return ResponseEntity.ok(Map.of(
            "suspended", !activeBans.isEmpty(),
            "bans", activeBans
        ));
    }

    @PostMapping("/appeal")
    public ResponseEntity<Void> appeal(@Valid @RequestBody SuspensionAppealRequest request) {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        List<Ban> activeBans = banRepository
            .findByUser_UserIdAndActiveTrue(user.getUserId())
            .stream()
            .filter(this::isActive)
            .toList();

        if (activeBans.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        String banSummary = activeBans.stream()
            .map(ban -> "escopo=" + ban.getScope() + ", motivo=" + ban.getReason()
                + ", expira=" + (ban.getExpiresAt() == null ? "permanente" : ban.getExpiresAt()))
            .reduce((left, right) -> left + " | " + right)
            .orElse("não informado");

        String subject = "SkillSwap - Contestação de suspensão de @" + user.getUsername();
        String body = "Uma contestação foi enviada pela plataforma SkillSwap.\n\n"
            + "Usuário: @" + user.getUsername() + "\n"
            + "E-mail: " + user.getEmail() + "\n"
            + "Situação registrada: " + banSummary + "\n\n"
            + "Mensagem da pessoa usuária:\n" + request.message().trim();

        emailService.sendNotification(supportEmail, subject, body);
        return ResponseEntity.accepted().build();
    }

    private boolean isActive(Ban ban) {
        return ban.isActive() && (ban.getExpiresAt() == null || ban.getExpiresAt().isAfter(Instant.now()));
    }

    private Map<String, Object> toResponse(Ban ban) {
        Map<String, Object> result = new HashMap<>();
        result.put("id", ban.getId());
        result.put("scope", ban.getScope());
        result.put("reason", ban.getReason());
        result.put("createdAt", ban.getCreatedAt());
        result.put("expiresAt", ban.getExpiresAt());
        return result;
    }
}
