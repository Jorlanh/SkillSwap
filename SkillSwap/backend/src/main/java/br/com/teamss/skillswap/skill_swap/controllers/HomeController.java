package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.*;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.ProfileRepository;
import br.com.teamss.skillswap.skill_swap.model.services.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final PostService postService;
    private final CommunityService communityService;
    private final NotificationService notificationService;
    private final ShareLinkRepository shareLinkRepository;
    private final ProfileRepository profileRepository;
    private final TrendingService trendingService;
    private final CommentService commentService;
    private final UserServiceDTO userServiceDTO;

    @Autowired
    public HomeController(PostService postService, CommunityService communityService,
                          NotificationService notificationService, ShareLinkRepository shareLinkRepository,
                          TrendingService trendingService, CommentService commentService, UserServiceDTO userServiceDTO,
                          ProfileRepository profileRepository) {
        this.postService = postService;
        this.communityService = communityService;
        this.notificationService = notificationService;
        this.shareLinkRepository = shareLinkRepository;
        this.profileRepository = profileRepository;
        this.trendingService = trendingService;
        this.commentService = commentService;
        this.userServiceDTO = userServiceDTO;
    }

    /**
     * CORREÇÃO TÁTICA: Endpoint solicitado pelo Frontend (api.ts)
     * Mapeia /api/home/trending para retornar os posts em alta.
     */
    @GetMapping("/trending")
    public ResponseEntity<List<PostResponseDTO>> getTrending(@RequestParam(defaultValue = "DAY") String period) {
        UUID viewerId = authenticatedUserIdOrNull();
        return ResponseEntity.ok(trendingService.getTrendingPosts(period, 10).stream().map(post -> toResponse(post, viewerId)).toList());
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long notificationId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        notificationService.markAsRead(notificationId, authenticatedUser.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/posts")
    public ResponseEntity<PostResponseDTO> createPost(
            @RequestParam(defaultValue = "") String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile video) throws IOException {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Post post = postService.createPost(authenticatedUser.getUserId(), title, content, image, video);
        return ResponseEntity.ok(toResponse(post, authenticatedUser.getUserId()));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<PostResponseDTO> likePost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(toResponse(postService.likePost(postId, authenticatedUser.getUserId()), authenticatedUser.getUserId()));
    }

    @PostMapping("/posts/{postId}/repost")
    public ResponseEntity<PostResponseDTO> repost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(toResponse(postService.repost(postId, authenticatedUser.getUserId()), authenticatedUser.getUserId()));
    }

    @PostMapping("/posts/{postId}/comment")
    public ResponseEntity<PostResponseDTO> commentOnPost(@PathVariable Long postId, @RequestParam String content,
                                                         @RequestParam(required = false) Long parentCommentId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        commentService.createComment(postId, authenticatedUser.getUserId(), content, parentCommentId);
        return ResponseEntity.ok(toResponse(postService.findAll().stream().filter(p -> postId.equals(p.getPostId())).findFirst().orElseThrow(), authenticatedUser.getUserId()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(notificationService.getAllNotifications(authenticatedUser.getUserId()));
    }

    @PostMapping("/communities/{communityId}/join")
    public ResponseEntity<Void> joinCommunity(@PathVariable UUID communityId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        communityService.joinCommunity(communityId, authenticatedUser.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/communities")
    public ResponseEntity<CommunitySummaryDTO> createCommunity(
            @Valid @RequestBody CreateCommunityRequest request) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(communityService.createCommunity(request, authenticatedUser.getUserId()));
    }
    
    @GetMapping("/posts")
    public ResponseEntity<List<PostResponseDTO>> getPosts(
            @RequestParam(defaultValue = "TRENDING") String sortBy,
            @RequestParam(defaultValue = "DAY") String period) {
        List<PostResponseDTO> posts = postService.getPosts(sortBy, period, authenticatedUserIdOrNull());
        posts.forEach(post -> {
            postService.incrementViewCount(post.getPostId());
            // Reflete a visualização atual na mesma resposta, sem esperar um
            // novo refresh para o contador aparecer atualizado.
            post.setViewsCount(post.getViewsCount() + 1);
        });
        return ResponseEntity.ok(posts);
    }

    private UUID authenticatedUserIdOrNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt)) return null;
        try {
            return userServiceDTO.getAuthenticatedUser().getUserId();
        } catch (RuntimeException ignored) {
            return null;
        }
    }
    
    @GetMapping("/posts/{postId}/share")
    public ResponseEntity<String> generateShareLink(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.generateShareLink(postId));
    }

    @GetMapping("/posts/{postId}")
    public ResponseEntity<PostResponseDTO> getPost(@PathVariable Long postId) {
        Post post = postService.findAll().stream()
                .filter(candidate -> postId.equals(candidate.getPostId()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Post não encontrado."));
        return ResponseEntity.ok(toResponse(post));
    }

    @GetMapping("/trending-topics")
    public ResponseEntity<List<String>> getTrendingTopics(@RequestParam(defaultValue = "DAY") String period) {
        return ResponseEntity.ok(postService.getTrendingTopics(period));
    }

    @GetMapping("/communities")
    public ResponseEntity<List<CommunitySummaryDTO>> getCommunities(@RequestParam(defaultValue = "5") int limit) {
        return ResponseEntity.ok(communityService.getCommunities(limit));
    }

    @PostMapping("/share/click")
    public ResponseEntity<Void> trackShareClick(@RequestParam String shareUrl) {
        ShareLink shareLink = shareLinkRepository.findByShareUrl(shareUrl)
                .orElseThrow(() -> new RuntimeException("Link de compartilhamento não encontrado!"));
        shareLink.setClickCount(shareLink.getClickCount() + 1);
        shareLinkRepository.save(shareLink);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/trending-posts")
    public ResponseEntity<List<PostResponseDTO>> getTrendingPosts(@RequestParam(defaultValue = "DAY") String period) {
        UUID viewerId = authenticatedUserIdOrNull();
        return ResponseEntity.ok(trendingService.getTrendingPosts(period, 10).stream().map(post -> toResponse(post, viewerId)).toList());
    }

    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<List<LikeDTO>> getLikesByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getLikesByPost(postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId, authenticatedUserIdOrNull()));
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Long> toggleCommentLike(@PathVariable Long commentId) {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(commentService.toggleLike(commentId, user.getUserId()));
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long commentId) {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        commentService.deleteComment(commentId, user.getUserId());
        return ResponseEntity.noContent().build();
    }

    private PostResponseDTO toResponse(Post post) {
        return toResponse(post, authenticatedUserIdOrNull());
    }

    private PostResponseDTO toResponse(Post post, UUID viewerId) {
        PostResponseDTO response = new PostResponseDTO();
        response.setPostId(post.getPostId());
        if (post.getUser() != null) {
            response.setUserId(post.getUser().getUserId());
        }
        response.setTitle(post.getTitle());
        response.setContent(post.getContent());
        response.setImageUrl(post.getImageUrl());
        response.setVideoUrl(post.getVideoUrl());
        response.setCreatedAt(post.getCreatedAt());
        response.setLikesCount(post.getLikesCount());
        response.setCommentsCount(post.getCommentsCount());
        response.setRepostsCount(post.getRepostsCount());
        response.setSharesCount(post.getSharesCount());
        response.setViewsCount(post.getViewsCount());
        if (viewerId != null) {
            response.setLikedByMe(postService.isLikedBy(post.getPostId(), viewerId));
            boolean isOwnPersistedRepost = post.getRepostOf() != null
                && post.getUser() != null
                && viewerId.equals(post.getUser().getUserId());
            response.setRepostedByMe(isOwnPersistedRepost || postService.isRepostedBy(post.getPostId(), viewerId));
        }
        if (post.getUser() != null) {
            UserSummaryDTO summary = new UserSummaryDTO(post.getUser().getUsername(), post.getUser().getName());
            summary.setUserId(post.getUser().getUserId());
            summary.setVerifiedBadge(post.getUser().isVerifiedBadge());
            Profile currentProfile = post.getProfile();
            if (currentProfile == null) {
                currentProfile = profileRepository.findByUser_UserId(post.getUser().getUserId()).orElse(null);
            }
            summary.setAvatarUrl(currentProfile == null ? null : currentProfile.getImageUrl());
            response.setUser(summary);
        }
        if (post.getCommunity() != null) {
            response.setCommunity(post.getCommunity().getName());
            response.setCommunityId(post.getCommunity().getCommunityId());
            response.setCommunityName(post.getCommunity().getName());
            response.setCommunityImageUrl(post.getCommunity().getImageUrl());
        }
        postService.populateRepostMetadata(post, response);
        return response;
    }
}
