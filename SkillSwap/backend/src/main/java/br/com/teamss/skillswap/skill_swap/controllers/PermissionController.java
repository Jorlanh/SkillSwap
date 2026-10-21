package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.PermissionStatus;
import br.com.teamss.skillswap.skill_swap.model.services.PermissionService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/permissions")
public class PermissionController {

    @Autowired
    private PermissionService permissionService;

    // INJEÇÃO DO SERVICE PARA OBTER O USUÁRIO AUTENTICADO DE FORMA SEGURA
    @Autowired
    private UserServiceDTO userServiceDTO;

    /**
     * Endpoint seguro para alternar o estado da permissão do usuário autenticado.
     * O userId é obtido a partir do token de segurança, não da requisição.
     */
    @PostMapping("/toggle/{permissionType}")
    public ResponseEntity<?> togglePermission(@PathVariable String permissionType) {
        // OBTÉM O USUÁRIO DIRETAMENTE DO CONTEXTO DE SEGURANÇA
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        String userId = authenticatedUser.getUserId().toString();

        permissionService.togglePermission(permissionType, userId);
        PermissionStatus status = permissionService.getPermissionStatus(permissionType, userId);
        return ResponseEntity.ok(new PermissionResponse(status));
    }

    /**
     * Endpoint seguro para definir a escolha de permissão do usuário autenticado.
     * O userId é obtido a partir do token de segurança, não da requisição.
     */
    @PostMapping("/choose/{permissionType}")
    public ResponseEntity<?> setPermissionChoice(
            @PathVariable String permissionType,
            @RequestParam String choice) { // REMOVIDO o @RequestParam String userId
        // OBTÉM O USUÁRIO DIRETAMENTE DO CONTEXTO DE SEGURANÇA
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        String userId = authenticatedUser.getUserId().toString();

        permissionService.setPermissionChoice(permissionType, userId, choice);
        PermissionStatus status = permissionService.getPermissionStatus(permissionType, userId);
        return ResponseEntity.ok(new PermissionResponse(status));
    }
}

// Classe de resposta para o frontend (inalterada)
class PermissionResponse {
    private PermissionStatus status;

    public PermissionResponse(PermissionStatus status) {
        this.status = status;
    }

    public PermissionStatus getStatus() {
        return status;
    }
}