package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.dto.PostResponseDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserSummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.services.PostService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import br.com.teamss.skillswap.skill_swap.model.services.FileUploadService;
import br.com.teamss.skillswap.skill_swap.model.services.CommentService;
import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private final PostService postService;
    private final UserServiceDTO userServiceDTO;
    private final FileUploadService fileUploadService;
    private final CommentService commentService;

    public PostController(PostService postService, UserServiceDTO userServiceDTO, FileUploadService fileUploadService, CommentService commentService) {
        this.postService = postService;
        this.userServiceDTO = userServiceDTO;
        this.fileUploadService = fileUploadService;
        this.commentService = commentService;
    }

    @GetMapping
    public ResponseEntity<List<PostResponseDTO>> getAllPosts() {
        return ResponseEntity.ok(postService.findAll().stream().map(this::toResponse).toList());
    }

    @PostMapping
    public ResponseEntity<?> createPost(@RequestParam("content") String content,
                                        @RequestParam(value = "image", required = false) MultipartFile image) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        
        Post newPost = new Post();
        newPost.setContent(content);
        
        try {
            if (image != null && !image.isEmpty()) {
                String imageUrl = fileUploadService.uploadFile(image);
                newPost.setImageUrl(imageUrl);
            }
            // Salvamento real no Banco de Dados
            Post savedPost = postService.save(newPost, authenticatedUser.getUserId());
            return ResponseEntity.ok(toResponse(savedPost));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Falha ao salvar postagem: " + e.getMessage());
        }
    }

    @PostMapping("/{postId}/like")
    public ResponseEntity<PostResponseDTO> like(@PathVariable Long postId) {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(toResponse(postService.likePost(postId, user.getUserId())));
    }

    @PostMapping("/{postId}/repost")
    public ResponseEntity<PostResponseDTO> repost(@PathVariable Long postId) {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        return ResponseEntity.ok(toResponse(postService.repost(postId, user.getUserId())));
    }

    @PostMapping("/{postId}/comment")
    public ResponseEntity<PostResponseDTO> comment(@PathVariable Long postId, @RequestParam String content,
                                                    @RequestParam(required = false) Long parentCommentId) {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        CommentDTO created = new CommentDTO();
        var comment = commentService.createComment(postId, user.getUserId(), content, parentCommentId);
        created.setCommentId(comment.getCommentId());
        created.setPostId(postId);
        created.setUserId(user.getUserId());
        created.setUsername(user.getUsername());
        created.setContent(comment.getContent());
        created.setCreatedAt(comment.getCreatedAt());
        return ResponseEntity.ok(toResponse(postService.findAll().stream().filter(p -> postId.equals(p.getPostId())).findFirst().orElseThrow()));
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId) {
        UserDTO user = userServiceDTO.getAuthenticatedUser();
        postService.deletePost(postId, user.getUserId());
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
            summary.setUserId(post.getUser().getUserId());
            summary.setVerifiedBadge(post.getUser().isVerifiedBadge());
            Profile currentProfile = post.getUser().getProfile() != null ? post.getUser().getProfile() : post.getProfile();
            summary.setAvatarUrl(currentProfile == null ? null : currentProfile.getImageUrl());
            response.setUser(summary);
        }
        if (post.getCommunity() != null) {
            response.setCommunity(post.getCommunity().getName());
            response.setCommunityId(post.getCommunity().getCommunityId());
            response.setCommunityName(post.getCommunity().getName());
            response.setCommunityImageUrl(post.getCommunity().getImageUrl());
        }
        return response;
    }
}
