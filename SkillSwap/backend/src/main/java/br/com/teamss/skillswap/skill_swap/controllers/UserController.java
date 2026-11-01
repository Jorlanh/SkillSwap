package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPrivateProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPublicProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final UserServiceDTO userServiceDTO;

    @Autowired
    private UserRepository userRepository;

    public UserController(UserService userService, UserServiceDTO userServiceDTO) {
        this.userServiceDTO = userServiceDTO;
        this.userService = userService;
    }

    @GetMapping
    public List<UserSummaryDTO> getAllUsers() {
        return userServiceDTO.findAllSummaries();
    }

    @GetMapping("/{username}")
    public ResponseEntity<UserPublicProfileDTO> getUserByUsername(@PathVariable String username) {
        return ResponseEntity.ok(userServiceDTO.findPublicProfileByUsername(username));
    }

    @GetMapping("/me")
    public ResponseEntity<UserPrivateProfileDTO> getMyProfile() {
        UserDTO authUser = userServiceDTO.getAuthenticatedUser();
        User user = userRepository.findById(authUser.getUserId())
            .orElseThrow(() -> new EntityNotFoundException("Usuário não sincronizado."));
        return ResponseEntity.ok(userServiceDTO.toUserPrivateProfileDTO(user));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UserDTO> updateUser(@PathVariable UUID id, @RequestBody User user) {
        User updatedUser = userService.update(id, user);
        return ResponseEntity.ok(userServiceDTO.toUserDTO(updatedUser));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUser(@PathVariable UUID id) {
        userService.delete(id);
        return ResponseEntity.noContent().build();
    }
}