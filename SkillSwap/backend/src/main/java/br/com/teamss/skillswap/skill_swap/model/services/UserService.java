package br.com.teamss.skillswap.skill_swap.model.services;

import java.util.List;
import java.util.UUID;
import br.com.teamss.skillswap.skill_swap.model.entities.User;

public interface UserService {
    List<User> findAll(); 
    User findById(UUID id); 
    User save(User user); 
    User addSkills(UUID id, List<Long> skillIds); 
    User addRoles(UUID id, List<Long> roleIds); 
    User update(UUID id, User user); 
    void delete(UUID id); 

    User updateAccessibilitySettings(UUID userId, boolean librasAvatarEnabled, String preferredTheme);

    // Incremento vital para sincronização de Perfil e Avatar
    void updateUserIdentityAndSkills(UUID userId, String name, String username, List<String> skillNames);
    void updateAvatar(UUID userId, String avatarUrl);
}