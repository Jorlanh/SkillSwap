package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.Role;
import br.com.teamss.skillswap.skill_swap.model.entities.Skill;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.RoleRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.SkillRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.UserService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SkillRepository skillRepository;
    private final ProfileRepository profileRepository;

    public UserServiceImpl(UserRepository userRepository, RoleRepository roleRepository, 
                           SkillRepository skillRepository, ProfileRepository profileRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.skillRepository = skillRepository;
        this.profileRepository = profileRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<User> findAll() {
        return userRepository.findAll();
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado com ID: " + id));
    }

    @Override
    @Transactional 
    public User save(User user) {
        if (user.getUserId() == null) {
            user.setUserId(UUID.randomUUID());
        }

        user.setRoles(manageRoles(user.getRoles()));
        user.setSkills(manageSkills(user.getSkills()));

        if (user.getProfile() != null) {
             if (user.getProfile().getProfileId() != null) {
                 var managedProfile = profileRepository.findById(user.getProfile().getProfileId())
                         .orElseThrow(() -> new EntityNotFoundException(
                                 "Profile não encontrado: " + user.getProfile().getProfileId()));
                 managedProfile.setUser(user);
                 user.setProfile(managedProfile);
             } else {
                 user.getProfile().setUser(user);
             }
        } else {
             Profile defaultProfile = new Profile();
             defaultProfile.setUser(user);
             user.setProfile(defaultProfile); 
        }

        return userRepository.save(user);
    }

    private Set<Role> manageRoles(Set<Role> requestedRoles) {
        if (requestedRoles == null || requestedRoles.isEmpty()) {
            return new HashSet<>();
        }
        return requestedRoles.stream()
            .map(role -> role.getRoleId() != null ? roleRepository.findById(role.getRoleId())
                    .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + role.getRoleId())) : role) 
            .collect(Collectors.toSet());
    }

    private Set<Skill> manageSkills(Set<Skill> requestedSkills) {
        if (requestedSkills == null || requestedSkills.isEmpty()) {
            return new HashSet<>();
        }
        return requestedSkills.stream()
            .map(skill -> skill.getSkillId() != null ? skillRepository.findById(skill.getSkillId())
                    .orElseThrow(() -> new EntityNotFoundException("Skill não encontrada: " + skill.getSkillId())) : skill) 
            .collect(Collectors.toSet());
    }

    @Override
    @Transactional 
    public User addSkills(UUID userId, List<Long> skillIds) {
        User user = findById(userId); 
        if (skillIds != null && !skillIds.isEmpty()) { 
            Set<Skill> skillsToAdd = skillIds.stream()
                    .map(id -> skillRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Skill não encontrada: " + id)))
                    .collect(Collectors.toSet());
            user.getSkills().addAll(skillsToAdd); 
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional 
    public User addRoles(UUID userId, List<Long> roleIds) {
        User user = findById(userId); 
         if (roleIds != null && !roleIds.isEmpty()) { 
            Set<Role> rolesToAdd = roleIds.stream()
                    .map(id -> roleRepository.findById(id)
                            .orElseThrow(() -> new EntityNotFoundException("Role não encontrada: " + id)))
                    .collect(Collectors.toSet());
            user.getRoles().addAll(rolesToAdd); 
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional 
    public User update(UUID id, User userUpdates) {
        User existingUser = findById(id); 

        if (StringUtils.hasText(userUpdates.getUsername())) existingUser.setUsername(userUpdates.getUsername());
        if (StringUtils.hasText(userUpdates.getName())) existingUser.setName(userUpdates.getName());
        if (StringUtils.hasText(userUpdates.getEmail())) existingUser.setEmail(userUpdates.getEmail());
        if (userUpdates.getBirthDate() != null) existingUser.setBirthDate(userUpdates.getBirthDate());
        if (StringUtils.hasText(userUpdates.getPhoneNumber())) existingUser.setPhoneNumber(userUpdates.getPhoneNumber());
        if (StringUtils.hasText(userUpdates.getBio())) existingUser.setBio(userUpdates.getBio());
        if (StringUtils.hasText(userUpdates.getCountry())) existingUser.setCountry(userUpdates.getCountry());
        if (StringUtils.hasText(userUpdates.getCity())) existingUser.setCity(userUpdates.getCity());
        if (StringUtils.hasText(userUpdates.getState())) existingUser.setState(userUpdates.getState());

        if (userUpdates.getRoles() != null) {
            existingUser.setRoles(manageRoles(userUpdates.getRoles()));
        }

        if (userUpdates.getSkills() != null) {
            existingUser.setSkills(manageSkills(userUpdates.getSkills()));
        }

        if (userUpdates.getProfile() != null) {
             Profile existingProfile = existingUser.getProfile();
             if (existingProfile == null) { 
                 existingProfile = new Profile();
                 existingProfile.setUser(existingUser);
                 existingUser.setProfile(existingProfile);
             }
             Profile profileUpdates = userUpdates.getProfile();
             if (StringUtils.hasText(profileUpdates.getDescription())) existingProfile.setDescription(profileUpdates.getDescription());
             if (StringUtils.hasText(profileUpdates.getImageUrl())) existingProfile.setImageUrl(profileUpdates.getImageUrl());
             if (StringUtils.hasText(profileUpdates.getLocation())) existingProfile.setLocation(profileUpdates.getLocation());
             if (StringUtils.hasText(profileUpdates.getContactInfo())) existingProfile.setContactInfo(profileUpdates.getContactInfo());
             if (StringUtils.hasText(profileUpdates.getSocialMediaLinks())) existingProfile.setSocialMediaLinks(profileUpdates.getSocialMediaLinks());
             if (StringUtils.hasText(profileUpdates.getAvailabilityStatus())) existingProfile.setAvailabilityStatus(profileUpdates.getAvailabilityStatus());
             if (StringUtils.hasText(profileUpdates.getInterests())) existingProfile.setInterests(profileUpdates.getInterests());
             if (StringUtils.hasText(profileUpdates.getExperienceLevel())) existingProfile.setExperienceLevel(profileUpdates.getExperienceLevel());
             if (StringUtils.hasText(profileUpdates.getEducationLevel())) existingProfile.setEducationLevel(profileUpdates.getEducationLevel());
             
             if (profileUpdates.getIsPrivate() != null) existingProfile.setIsPrivate(profileUpdates.getIsPrivate());
        }

        existingUser.setLibrasAvatarEnabled(userUpdates.isLibrasAvatarEnabled());
        if (StringUtils.hasText(userUpdates.getPreferredTheme())) {
             existingUser.setPreferredTheme(userUpdates.getPreferredTheme());
        }

        return userRepository.save(existingUser);
    }

    @Override
    @Transactional 
    public void delete(UUID id) {
        if (!userRepository.existsById(id)) { 
             throw new EntityNotFoundException("Usuário não encontrado para exclusão com ID: " + id);
        }
        userRepository.deleteById(id);
    }

    @Override
    @Transactional 
    public User updateAccessibilitySettings(UUID userId, boolean librasAvatarEnabled, String preferredTheme) {
        User user = findById(userId); 
        user.setLibrasAvatarEnabled(librasAvatarEnabled);

        if (StringUtils.hasText(preferredTheme) && List.of("default", "high-contrast-dark", "high-contrast-light").contains(preferredTheme)) {
            user.setPreferredTheme(preferredTheme);
        } else if (preferredTheme != null) {
            System.err.println("WARN: Tentativa de definir tema de acessibilidade inválido: " + preferredTheme);
        }
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserIdentityAndSkills(UUID userId, String name, String username, List<String> skillNames) {
        User user = findById(userId);

        if (StringUtils.hasText(name)) {
            user.setName(name);
        }
        
        if (StringUtils.hasText(username)) {
            user.setUsername(username);
        }

        if (skillNames != null) {
            Set<Skill> newSkills = new HashSet<>();
            for (String skillName : skillNames) {
                String cleanName = skillName.trim();
                if (!cleanName.isEmpty()) {
                    Skill skill = skillRepository.findByNameIgnoreCase(cleanName)
                        .orElseGet(() -> {
                            Skill newSkill = new Skill();
                            newSkill.setName(cleanName);
                            return skillRepository.save(newSkill);
                        });
                    newSkills.add(skill);
                }
            }
            user.setSkills(newSkills);
        }

        userRepository.save(user);
    }

    // --- IMPLEMENTAÇÃO DO MÉTODO QUE FALTAVA ---
    @Override
    @Transactional
    public void updateAvatar(UUID userId, String avatarUrl) {
        User user = findById(userId);
        Profile profile = user.getProfile();
        
        if (profile == null) {
            profile = new Profile();
            profile.setUser(user);
            user.setProfile(profile);
        }
        
        // No seu backend, o campo da foto no Profile é imageUrl
        profile.setImageUrl(avatarUrl);
        userRepository.save(user);
    }
}