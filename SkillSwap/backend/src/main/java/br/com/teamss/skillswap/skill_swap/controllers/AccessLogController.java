package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.AccessLogDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.AccessLocationRequest;
import br.com.teamss.skillswap.skill_swap.model.services.AccessLogService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/access-logs")
public class AccessLogController {

    @Autowired
    private AccessLogService accessLogService;

    @Autowired
    private UserServiceDTO userServiceDTO;

    /**
     * Endpoint seguro que retorna o histórico de acessos APENAS do utilizador autenticado.
     * O ID do utilizador é obtido de forma segura a partir do token de autenticação.
     */
    @GetMapping("/me")
    public ResponseEntity<List<AccessLogDTO>> getMyAccessHistory() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        List<AccessLogDTO> history = accessLogService.getAccessHistory(authenticatedUser.getUserId());
        return ResponseEntity.ok(history);
    }

    @PostMapping("/me/location")
    public ResponseEntity<Void> updateMyLocation(@jakarta.validation.Valid @RequestBody AccessLocationRequest request) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        accessLogService.updateLatestLocation(authenticatedUser.getUserId(), request.latitude(), request.longitude());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/me/{accessId}")
    public ResponseEntity<Void> revokeMyAccess(@PathVariable Long accessId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        accessLogService.revokeAccess(authenticatedUser.getUserId(), accessId);
        return ResponseEntity.noContent().build();
    }
}
