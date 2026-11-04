package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.*;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.services.*;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/home")
public class HomeController {

    private final PostService postService;
    private final CommunityService communityService;
    private final NotificationService notificationService;
    private final ShareLinkRepository shareLinkRepository;
    private final TrendingService trendingService;
    private final CommentService commentService;
    private final UserServiceDTO userServiceDTO;

    @Autowired
    public HomeController(PostService postService, CommunityService communityService,
                          NotificationService notificationService, ShareLinkRepository shareLinkRepository,
                          TrendingService trendingService, CommentService commentService, UserServiceDTO userServiceDTO) {
        this.postService = postService;
        this.communityService = communityService;
        this.notificationService = notificationService;
        this.shareLinkRepository = shareLinkRepository;
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
        return ResponseEntity.ok(trendingService.getTrendingPosts(period, 10).stream().map(this::toResponse).toList());
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
        return ResponseEntity.ok(toResponse(post));
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<PostResponseDTO> likePost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(toResponse(postService.likePost(postId, authenticatedUser.getUserId())));
    }

    @PostMapping("/posts/{postId}/repost")
    public ResponseEntity<PostResponseDTO> repost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(toResponse(postService.repost(postId, authenticatedUser.getUserId())));
    }

    @PostMapping("/posts/{postId}/comment")
    public ResponseEntity<PostResponseDTO> commentOnPost(@PathVariable Long postId, @RequestParam String content,
                                                         @RequestParam(required = false) Long parentCommentId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        commentService.createComment(postId, authenticatedUser.getUserId(), content, parentCommentId);
        return ResponseEntity.ok(toResponse(postService.findAll().stream().filter(p -> postId.equals(p.getPostId())).findFirst().orElseThrow()));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(authenticatedUser.getUserId()));
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
        List<PostResponseDTO> posts = postService.getPosts(sortBy, period);
        posts.forEach(post -> postService.incrementViewCount(post.getPostId()));
        return ResponseEntity.ok(posts);
    }
    
    @GetMapping("/posts/{postId}/share")
    public ResponseEntity<String> generateShareLink(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.generateShareLink(postId));
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
        return ResponseEntity.ok(trendingService.getTrendingPosts(period, 10).stream().map(this::toResponse).toList());
    }

    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<List<LikeDTO>> getLikesByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getLikesByPost(postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
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
        if (post.getUser() != null) {
            UserSummaryDTO summary = new UserSummaryDTO(post.getUser().getUsername(), post.getUser().getName());
            summary.setVerifiedBadge(post.getUser().isVerifiedBadge());
            response.setUser(summary);
        }
        if (post.getCommunity() != null) response.setCommunity(post.getCommunity().getName());
        return response;
    }
}
