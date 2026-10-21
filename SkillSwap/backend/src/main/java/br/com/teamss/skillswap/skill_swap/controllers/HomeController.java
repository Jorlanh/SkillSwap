package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.*;
import br.com.teamss.skillswap.skill_swap.model.entities.Community;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import br.com.teamss.skillswap.skill_swap.model.repositories.ShareLinkRepository;
import br.com.teamss.skillswap.skill_swap.model.services.*;
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
    public ResponseEntity<List<Post>> getTrending(@RequestParam(defaultValue = "DAY") String period) {
        return ResponseEntity.ok(trendingService.getTrendingPosts(period, 10));
    }

    @PostMapping("/notifications/{notificationId}/read")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable Long notificationId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        notificationService.markAsRead(notificationId, authenticatedUser.getUserId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/posts")
    public ResponseEntity<Post> createPost(
            @RequestParam String title,
            @RequestParam String content,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam(required = false) MultipartFile video) throws IOException {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Post post = postService.createPost(authenticatedUser.getUserId(), title, content, image, video);
        return ResponseEntity.ok(post);
    }

    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<Post> likePost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(postService.likePost(postId, authenticatedUser.getUserId()));
    }

    @PostMapping("/posts/{postId}/repost")
    public ResponseEntity<Post> repost(@PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(postService.repost(postId, authenticatedUser.getUserId()));
    }

    @PostMapping("/posts/{postId}/comment")
    public ResponseEntity<Post> commentOnPost(@PathVariable Long postId, @RequestParam String content) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(postService.commentOnPost(postId, authenticatedUser.getUserId(), content));
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<NotificationDTO>> getUnreadNotifications() {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(notificationService.getUnreadNotifications(authenticatedUser.getUserId()));
    }

    @PostMapping("/communities/{communityId}/join")
    public ResponseEntity<Community> joinCommunity(@PathVariable UUID communityId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(communityService.joinCommunity(communityId, authenticatedUser.getUserId()));
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
    public ResponseEntity<List<Community>> getCommunities(@RequestParam(defaultValue = "5") int limit) {
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
    public ResponseEntity<List<Post>> getTrendingPosts(@RequestParam(defaultValue = "DAY") String period) {
        return ResponseEntity.ok(trendingService.getTrendingPosts(period, 10));
    }

    @GetMapping("/posts/{postId}/likes")
    public ResponseEntity<List<LikeDTO>> getLikesByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(postService.getLikesByPost(postId));
    }

    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<List<CommentDTO>> getCommentsByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(commentService.getCommentsByPost(postId));
    }
}