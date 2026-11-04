package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.LikeDTO;
import br.com.teamss.skillswap.skill_swap.dto.PostResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.*;
import br.com.teamss.skillswap.skill_swap.model.exception.InappropriateContentException;
import br.com.teamss.skillswap.skill_swap.model.repositories.*;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.services.PostService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    @Value("${app.frontend-url:http://localhost:5173}")
    private String frontendUrl;

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final NotificationService notificationService;
    private final LikeRepository likeRepository;
    private final RepostRepository repostRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final CommentRepository commentRepository;
    private final FileUploadService fileUploadService;
    private final ContentModerationService moderationService;

    // Injeção via construtor (Recomendado)
    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository,
                           ProfileRepository profileRepository,
                           NotificationService notificationService, LikeRepository likeRepository,
                           RepostRepository repostRepository, ShareLinkRepository shareLinkRepository,
                           CommentRepository commentRepository, FileUploadService fileUploadService,
                           ContentModerationService moderationService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.profileRepository = profileRepository;
        this.notificationService = notificationService;
        this.likeRepository = likeRepository;
        this.repostRepository = repostRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.commentRepository = commentRepository;
        this.fileUploadService = fileUploadService;
        this.moderationService = moderationService;
    }

    @Override
    @Transactional
    public Post createPost(UUID userId, String title, String content, MultipartFile image, MultipartFile video) {
        if (moderationService.isContentInappropriate(title) || moderationService.isContentInappropriate(content)) {
            throw new InappropriateContentException("O seu post viola as diretrizes da comunidade.");
        }
        
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        int maxPost = 1000 + (user.getProfile() == null ? 0 : user.getProfile().getExtraPostChars());
        if (content == null || content.isBlank()) throw new IllegalArgumentException("O post precisa ter conteudo.");
        if (content.length() > maxPost) {
            throw new IllegalArgumentException("O post ultrapassa o limite gratuito de " + maxPost + " caracteres.");
        }
        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setProfile(ensureProfile(user));
        post.setCreatedAt(Instant.now());

        try {
            if (image != null && !image.isEmpty()) {
                post.setImageUrl(fileUploadService.uploadFile(image));
            }
            if (video != null && !video.isEmpty()) {
                post.setVideoUrl(fileUploadService.uploadFile(video));
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao processar mídia do post: " + e.getMessage());
        }

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post save(Post post, UUID userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado"));
        post.setUser(user);
        if (post.getProfile() == null) post.setProfile(ensureProfile(user));
        if (post.getTitle() == null) post.setTitle("");
        if (post.getContent() == null || post.getContent().isBlank()) {
            throw new IllegalArgumentException("O post precisa ter conteúdo.");
        }
        if (post.getCreatedAt() == null) post.setCreatedAt(Instant.now());
        return postRepository.save(post);
    }

    private Profile ensureProfile(User user) {
        if (user.getProfile() != null) return user.getProfile();
        Profile profile = profileRepository.findByUser_UserId(user.getUserId()).orElseGet(() -> {
            Profile created = new Profile();
            created.setUser(user);
            return profileRepository.save(created);
        });
        user.setProfile(profile);
        return profile;
    }

    @Override
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    @Override
    @Transactional
    public Post commentOnPost(Long postId, UUID userId, String content) {
        if (moderationService.isContentInappropriate(content)) {
            throw new InappropriateContentException("Comentário inadequado.");
        }
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        
        Comment newComment = new Comment();
        newComment.setPost(post);
        newComment.setUser(user);
        newComment.setContent(content);
        commentRepository.save(newComment);
        
        post.setCommentsCount(post.getCommentsCount() + 1);
        notificationService.createNotification(post.getUser().getUserId(), "Seu post recebeu um comentário de " + user.getUsername());
        
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post likePost(Long postId, UUID userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (likeRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            likeRepository.deleteByPost_PostIdAndUser_UserId(postId, userId);
            post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
        } else {
            likeRepository.save(new Like(post, user));
            post.setLikesCount(post.getLikesCount() + 1);
            notificationService.createNotification(post.getUser().getUserId(), "Seu post foi curtido por " + user.getUsername());
        }
        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post repost(Long postId, UUID userId) {
        Post originalPost = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (repostRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            repostRepository.deleteByPost_PostIdAndUser_UserId(postId, userId);
            originalPost.setRepostsCount(Math.max(0, originalPost.getRepostsCount() - 1));
        } else {
            repostRepository.save(new Repost(originalPost, user));
            originalPost.setRepostsCount(originalPost.getRepostsCount() + 1);
            notificationService.createNotification(originalPost.getUser().getUserId(), "Seu post foi repostado por " + user.getUsername());
        }

        return postRepository.save(originalPost);
    }

    @Override
    @Transactional
    public String generateShareLink(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        String base = frontendUrl == null ? "http://localhost:5173" : frontendUrl.replaceAll("/+$", "");
        String shareUrl = base + "/post/" + postId;
        
        ShareLink shareLink = new ShareLink();
        shareLink.setPost(post);
        shareLink.setUser(post.getUser());
        shareLink.setShareUrl(shareUrl);
        shareLinkRepository.save(shareLink);
        
        post.setSharesCount(post.getSharesCount() + 1);
        postRepository.save(post);

        return shareUrl;
    }

    @Override
    @Transactional
    public void deletePost(Long postId, UUID userId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado."));
        if (post.getUser() == null || !post.getUser().getUserId().equals(userId)) {
            throw new org.springframework.security.access.AccessDeniedException("Você só pode excluir suas próprias publicações.");
        }
        postRepository.delete(post);
    }

    @Override
    public List<PostResponseDTO> getPosts(String sortBy, String period) {
        Instant startTime = calculateStartTime(period);
        List<Post> posts = postRepository.findTrendingPosts(startTime);

        // Lógica de ordenação robusta
        switch (sortBy.toUpperCase()) {
            case "LIKES" -> posts.sort(Comparator.comparingInt(Post::getLikesCount).reversed());
            case "REPOSTS" -> posts.sort(Comparator.comparingInt(Post::getRepostsCount).reversed());
            case "COMMENTS" -> posts.sort(Comparator.comparingInt(Post::getCommentsCount).reversed());
            case "SHARES" -> posts.sort(Comparator.comparingInt(Post::getSharesCount).reversed());
            case "DATE" -> posts.sort(Comparator.comparing(Post::getCreatedAt).reversed());
            default -> {} 
        }

        return posts.stream().map(post -> {
            PostResponseDTO dto = new PostResponseDTO();
            dto.setPostId(post.getPostId());
            if (post.getUser() != null) {
                dto.setUserId(post.getUser().getUserId());
            }
            dto.setTitle(post.getTitle());
            dto.setContent(post.getContent());
            dto.setUser(new UserSummaryDTO(post.getUser().getUsername(), post.getUser().getName()));
            dto.setImageUrl(post.getImageUrl());
            dto.setVideoUrl(post.getVideoUrl());
            dto.setCreatedAt(post.getCreatedAt());
            dto.setLikesCount(post.getLikesCount());
            dto.setCommentsCount(post.getCommentsCount());
            dto.setRepostsCount(post.getRepostsCount());
            dto.setSharesCount(post.getSharesCount());
            dto.setViewsCount(post.getViewsCount());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    public List<String> getTrendingTopics(String period) {
        Instant startTime = calculateStartTime(period);
        List<Post> posts = postRepository.findTrendingPosts(startTime);
        Map<String, Double> topicScores = new HashMap<>();

        for (Post post : posts) {
            String[] words = post.getContent().toLowerCase().split("\\W+");
            for (String word : words) {
                if (word.length() > 3) {
                    double score = (post.getLikesCount() + post.getRepostsCount() +
                            post.getCommentsCount() + post.getSharesCount()) / 4.0;
                    // Correção de Null Safety no merge
                    topicScores.merge(word, score, Double::sum);
                }
            }
        }

        return topicScores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        post.setViewsCount(post.getViewsCount() + 1);
        postRepository.save(post);
    }
    
    @Override
    public List<LikeDTO> getLikesByPost(Long postId) {
        List<Like> likes = likeRepository.findAllByPost_PostId(postId);
        return likes.stream()
                .map(like -> {
                    User user = like.getUser();
                    UserSummaryDTO userSummary = new UserSummaryDTO(user.getUserId(), user.getUsername(), user.getName());
                    return new LikeDTO(userSummary, like.getCreatedAt());
                })
                .collect(Collectors.toList());
    }

    private Instant calculateStartTime(String period) {
        Instant now = Instant.now();
        return switch (period.toUpperCase()) {
            case "ALL" -> Instant.EPOCH;
            case "WEEK" -> now.minus(7, ChronoUnit.DAYS);
            case "MONTH" -> now.minus(30, ChronoUnit.DAYS);
            default -> now.minus(1, ChronoUnit.DAYS);
        };
    }
}
