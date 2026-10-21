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
import br.com.teamss.skillswap.skill_swap.model.services.ModerationAssessment;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final LikeRepository likeRepository;
    private final RepostRepository repostRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final CommentRepository commentRepository;
    private final FileUploadService fileUploadService;
    private final ContentModerationService moderationService;
    private final ReportService reportService;

    // Injeção via construtor (Recomendado)
    public PostServiceImpl(PostRepository postRepository, UserRepository userRepository,
                           NotificationService notificationService, LikeRepository likeRepository,
                           RepostRepository repostRepository, ShareLinkRepository shareLinkRepository,
                           CommentRepository commentRepository, FileUploadService fileUploadService,
                           ContentModerationService moderationService, ReportService reportService) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.notificationService = notificationService;
        this.likeRepository = likeRepository;
        this.repostRepository = repostRepository;
        this.shareLinkRepository = shareLinkRepository;
        this.commentRepository = commentRepository;
        this.fileUploadService = fileUploadService;
        this.moderationService = moderationService;
        this.reportService = reportService;
    }

    @Override
    @Transactional
    public Post createPost(UUID userId, String title, String content, MultipartFile image, MultipartFile video) {
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        ModerationAssessment titleAssessment = moderationService.assess(title);
        ModerationAssessment contentAssessment = moderationService.assess(content);
        if (titleAssessment.blocked() || contentAssessment.blocked()) {
            ModerationAssessment assessment = contentAssessment.blocked() ? contentAssessment : titleAssessment;
            reportService.createAutomatedCase(user, content, "post", assessment);
            throw new InappropriateContentException("O seu post viola as diretrizes da comunidade.");
        }
        int maxPost = 1000 + (user.getProfile() == null ? 0 : user.getProfile().getExtraPostChars());
        if (content == null || content.isBlank()) throw new IllegalArgumentException("O post precisa ter conteudo.");
        if (content.length() > maxPost) {
            throw new IllegalArgumentException("O post ultrapassa o limite técnico gratuito de " + maxPost + " caracteres.");
        }
        Post post = new Post();
        post.setUser(user);
        post.setTitle(title);
        post.setContent(content);
        post.setProfile(user.getProfile());
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
        if (post.getCreatedAt() == null) post.setCreatedAt(Instant.now());
        return postRepository.save(post);
    }

    @Override
    public List<Post> findAll() {
        return postRepository.findAll();
    }

    @Override
    @Transactional
    public Post commentOnPost(Long postId, UUID userId, String content) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));
        ModerationAssessment assessment = moderationService.assess(content);
        if (assessment.blocked()) {
            reportService.createAutomatedCase(user, content, "comentário", assessment);
            throw new InappropriateContentException("Comentário inadequado.");
        }
        
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
            throw new IllegalStateException("O usuário já curtiu este post.");
        }

        likeRepository.save(new Like(post, user));
        post.setLikesCount(post.getLikesCount() + 1);
        
        notificationService.createNotification(post.getUser().getUserId(), "Seu post foi curtido por " + user.getUsername());

        return postRepository.save(post);
    }

    @Override
    @Transactional
    public Post repost(Long postId, UUID userId) {
        Post originalPost = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        if (!repostRepository.existsByPost_PostIdAndUser_UserId(postId, userId)) {
            repostRepository.save(new Repost(originalPost, user));
            originalPost.setRepostsCount(originalPost.getRepostsCount() + 1);
            postRepository.save(originalPost);
            
            notificationService.createNotification(originalPost.getUser().getUserId(), "Seu post foi repostado por " + user.getUsername());
        }

        Post repostPost = new Post();
        repostPost.setUser(user);
        repostPost.setTitle("Repost: " + originalPost.getTitle());
        repostPost.setContent(originalPost.getContent());
        repostPost.setProfile(user.getProfile());
        repostPost.setCreatedAt(Instant.now());
        postRepository.save(repostPost);

        return originalPost;
    }

    @Override
    @Transactional
    public String generateShareLink(Long postId) {
        Post post = postRepository.findById(postId).orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        String shareUrl = "https://skillswap.com/share/post/" + UUID.randomUUID();
        
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
            case "WEEK" -> now.minus(7, ChronoUnit.DAYS);
            case "MONTH" -> now.minus(30, ChronoUnit.DAYS);
            default -> now.minus(1, ChronoUnit.DAYS);
        };
    }
}
