package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.ProfileService;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProfileServiceImpl implements ProfileService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private FileUploadService fileUploadService;

    // Construtor removido para evitar conflito com @Autowired nos campos
    // O Spring Boot injetará as dependências automaticamente.

    @Override
    public List<Profile> findAll() {
        return profileRepository.findAll();
    }

    @Override
    public Optional<Profile> findById(Long id) {
        return profileRepository.findById(id);
    }

    @Override
    public Optional<Profile> findByUserId(UUID userId) {
        return profileRepository.findByUser_UserId(userId);
    }

    @Override
    @Transactional
    public Profile createProfile(Profile profile, UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        profile.setUser(user);
        user.setProfile(profile);
        return profileRepository.save(profile);
    }

    @Override
    @Transactional
    public Profile save(Profile profile) {
        return profileRepository.save(profile);
    }

    @Override
    @Transactional
    public Profile update(Long id, Profile profileDetails) {
        Profile existingProfile = profileRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Perfil não encontrado com id: " + id));

        String description = profileDetails.getDescription();
        int maxBio = 500 + existingProfile.getExtraBioChars();
        if (description != null && description.length() > maxBio) {
            throw new IllegalArgumentException("A biografia ultrapassa o limite técnico gratuito de " + maxBio + " caracteres.");
        }
        existingProfile.setDescription(description);
        existingProfile.setImageUrl(profileDetails.getImageUrl());
        existingProfile.setLocation(profileDetails.getLocation());
        existingProfile.setContactInfo(profileDetails.getContactInfo());
        existingProfile.setSocialMediaLinks(profileDetails.getSocialMediaLinks());
        existingProfile.setAvailabilityStatus(profileDetails.getAvailabilityStatus());
        existingProfile.setInterests(profileDetails.getInterests());
        existingProfile.setExperienceLevel(profileDetails.getExperienceLevel());
        existingProfile.setEducationLevel(profileDetails.getEducationLevel());
        
        return profileRepository.save(existingProfile);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        profileRepository.deleteById(id);
    }

    @Override
    public List<Post> getFeed(UUID userId) {
        List<Post> ownPosts = postRepository.findByUserUserId(userId);
        List<Post> reposts = postRepository.findByRepostOfIsNotNullAndUserUserId(userId);
        return Stream.concat(ownPosts.stream(), reposts.stream())
                .sorted((p1, p2) -> p2.getCreatedAt().compareTo(p1.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void createPost(UUID userId, String content, MultipartFile image) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        
        Profile profile = profileRepository.findByUser_UserId(userId).orElse(null);
        int maxPost = 1000 + (profile == null ? 0 : profile.getExtraPostChars());
        if (content == null || content.isBlank()) throw new IllegalArgumentException("O post precisa ter conteudo.");
        if (content.length() > maxPost) {
            throw new IllegalArgumentException("O post ultrapassa o limite técnico gratuito de " + maxPost + " caracteres.");
        }
        Post post = new Post();
        post.setUser(user);
        post.setContent(content);
        post.setCreatedAt(Instant.now());
        post.setLikesCount(0);
        post.setCommentsCount(0);
        post.setRepostsCount(0);
        post.setSharesCount(0);
        post.setViewsCount(0);

        if (image != null && !image.isEmpty()) {
            try {
                String imageUrl = fileUploadService.uploadFile(image);
                post.setImageUrl(imageUrl);
            } catch (IOException e) {
                throw new RuntimeException("Falha ao processar upload da imagem: " + e.getMessage());
            }
        }

        postRepository.save(post);
    }

    @Override
    @Transactional
    public void deletePost(Long postId, UUID userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new EntityNotFoundException("Post não encontrado"));
        
        if (!post.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Você não tem permissão para deletar este post");
        }

        postRepository.delete(post);
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado"));
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);
    }

    @Override
    @Transactional
    public Post likePost(Long postId, UUID userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado"));
        post.setLikesCount(post.getLikesCount() + 1);
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post commentOnPost(Long postId, UUID userId, String content) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado"));
        post.setCommentsCount(post.getCommentsCount() + 1);
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post repost(Long postId, UUID userId) {
        Post originalPost = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado"));
        
        Post repost = new Post();
        repost.setUser(userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado")));
        repost.setRepostOf(originalPost.getPostId());
        repost.setContent(originalPost.getContent()); // Reposts costumam manter o conteúdo original
        repost.setCreatedAt(Instant.now());
        repost.setLikesCount(0);
        repost.setRepostsCount(0);
        repost.setCommentsCount(0);
        repost.setSharesCount(0);
        repost.setViewsCount(0);
        
        return postRepository.save(repost);
    }

    @Override
    public String generateShareLink(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado"));
        if (post.getShareUrl() == null) {
            post.setShareUrl("https://skillswap.com/share/" + UUID.randomUUID());
            postRepository.save(post);
        }
        return post.getShareUrl();
    }

    @Override
    public List<Post> getCommunityPosts(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        // Se a entidade User tiver getCommunityIds, senão ajuste para sua lógica de ManyToMany
        return postRepository.findByCommunity_CommunityIdIn(user.getCommunityIds());
    }

    @Override
    public List<String> getAchievements(UUID userId) {
        return List.of("Primeiro Post", "10 Seguidores");
    }

    @Override
    public List<Post> getActivity(UUID userId) {
        return postRepository.findByUserUserId(userId).stream().limit(5).collect(Collectors.toList());
    }

    @Override
    public List<Post> getHighlights(UUID userId) {
        return postRepository.findByUserUserId(userId).stream()
                .filter(p -> p.getLikesCount() > 10)
                .limit(3)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void followUser(UUID userId, UUID targetUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        User target = userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("Usuário alvo não encontrado"));
        if (!user.getFollowingIds().contains(targetUserId)) {
            user.getFollowingIds().add(targetUserId);
            target.setFollowers(target.getFollowers() + 1);
            userRepository.save(user);
            userRepository.save(target);
        }
    }

    @Override
    @Transactional
    public void unfollowUser(UUID userId, UUID targetUserId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        User target = userRepository.findById(targetUserId).orElseThrow(() -> new RuntimeException("Usuário alvo não encontrado"));
        if (user.getFollowingIds().contains(targetUserId)) {
            user.getFollowingIds().remove(targetUserId);
            target.setFollowers(Math.max(0, target.getFollowers() - 1));
            userRepository.save(user);
            userRepository.save(target);
        }
    }

    @Override
    @Transactional
    public User updateProfile(UUID userId, User updatedUser) {
        User user = userRepository.findById(userId).orElseThrow(() -> new EntityNotFoundException("Usuário não encontrado"));
        user.setName(updatedUser.getName());
        user.setSkills(updatedUser.getSkills() != null ? updatedUser.getSkills() : user.getSkills());
        user.setBio(updatedUser.getBio() != null ? updatedUser.getBio() : user.getBio());
        user.setCountry(updatedUser.getCountry() != null ? updatedUser.getCountry() : user.getCountry());
        user.setCity(updatedUser.getCity() != null ? updatedUser.getCity() : user.getCity());
        user.setState(updatedUser.getState() != null ? updatedUser.getState() : user.getState());
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void sendMessage(UUID senderId, UUID receiverId, String content, String type) {
        User sender = userRepository.findById(senderId).orElseThrow(() -> new RuntimeException("Remetente não encontrado"));
        User receiver = userRepository.findById(receiverId).orElseThrow(() -> new RuntimeException("Destinatário não encontrado"));
        UUID messageId = UUID.randomUUID();
        sender.getMessageIds().add(messageId);
        receiver.getMessageIds().add(messageId);
        userRepository.save(sender);
        userRepository.save(receiver);
    }

    @Override
    public void scheduleLesson(UUID userId, String calendarEvent) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        System.out.println("Aula agendada para " + user.getName() + ": " + calendarEvent);
    }

    @Override
    public List<UUID> getMessages(UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        return user.getMessageIds();
    }
}
