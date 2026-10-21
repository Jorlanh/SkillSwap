package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPrivateProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserPublicProfileDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserUpdateRequest;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
    public ResponseEntity<?> updateUser(@PathVariable UUID id, @Valid @RequestBody UserUpdateRequest request,
                                        Authentication authentication) {
        UserDTO current = userServiceDTO.getAuthenticatedUser();
        boolean admin = authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!admin && !id.equals(current.getUserId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "Você só pode alterar a própria conta."));
        }
        User user = new User();
        user.setUsername(request.username());
        user.setName(request.name());
        user.setBirthDate(request.birthDate());
        user.setPhoneNumber(request.phoneNumber());
        user.setBio(request.bio());
        user.setCountry(request.country());
        user.setCity(request.city());
        user.setState(request.state());
        user.setLibrasAvatarEnabled(request.librasAvatarEnabled());
        user.setPreferredTheme(request.preferredTheme());
        // A atualização de perfil não deve apagar relações que não fazem parte
        // deste contrato. O serviço interpreta null como "não alterar".
        user.setRoles(null);
        user.setSkills(null);
        if (request.profile() != null) {
            UserUpdateRequest.ProfileUpdateRequest profileRequest = request.profile();
            Profile profile = new Profile();
            profile.setDescription(profileRequest.description());
            profile.setLocation(profileRequest.location());
            profile.setContactInfo(profileRequest.contactInfo());
            profile.setSocialMediaLinks(profileRequest.socialMediaLinks());
            profile.setAvailabilityStatus(profileRequest.availabilityStatus());
            profile.setInterests(profileRequest.interests());
            profile.setExperienceLevel(profileRequest.experienceLevel());
            profile.setEducationLevel(profileRequest.educationLevel());
            profile.setIsPrivate(profileRequest.isPrivate());
            user.setProfile(profile);
        }
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
