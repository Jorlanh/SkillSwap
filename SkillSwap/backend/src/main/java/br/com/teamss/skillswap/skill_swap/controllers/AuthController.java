package br.com.teamss.skillswap.skill_swap.controllers;

import org.springframework.http.ResponseEntity;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserServiceDTO userServiceDTO;

    public AuthController(UserServiceDTO userServiceDTO) {
        this.userServiceDTO = userServiceDTO;
    }

    /**
     * A autenticação é emitida exclusivamente pelo Firebase Authentication.
     * Esta rota serve apenas para o cliente confirmar a identidade já validada
     * pelo resource server; ela nunca emite ou simula um JWT.
     */
    @GetMapping("/session")
    public ResponseEntity<UserDTO> currentSession() {
        return ResponseEntity.ok(userServiceDTO.getAuthenticatedUser());
    }
}
