package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.*;
import br.com.teamss.skillswap.skill_swap.model.entities.*;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/communities")
public class CommunityController {

    @Autowired
    private CommunityService communityService;

    @Autowired
    private UserServiceDTO userServiceDTO;

    @PostMapping("/{communityId}/join")
    public ResponseEntity<Community> joinCommunity(@PathVariable UUID communityId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Community community = communityService.joinCommunity(communityId, authenticatedUser.getUserId());
        return ResponseEntity.ok(community);
    }

    @GetMapping("/{communityId}/posts")
    public ResponseEntity<List<PostDTO>> getCommunityPosts(@PathVariable UUID communityId) {
        List<Post> posts = communityService.getCommunityPosts(communityId);
        List<PostDTO> postDTOs = posts.stream().map(this::convertToPostDTO).collect(Collectors.toList());
        return ResponseEntity.ok(postDTOs);
    }

    @PostMapping("/{communityId}/posts")
    public ResponseEntity<PostDTO> createCommunityPost(
            @PathVariable UUID communityId,
            @RequestParam("title") String title,
            @RequestParam("content") String content,
            @RequestParam(value = "image", required = false) MultipartFile image,
            @RequestParam(value = "video", required = false) MultipartFile video) throws IOException {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Post post = communityService.createCommunityPost(communityId, authenticatedUser.getUserId(), title, content, image, video);
        return ResponseEntity.ok(convertToPostDTO(post));
    }

    @PostMapping("/{communityId}/posts/{postId}/like")
    public ResponseEntity<Like> likePost(@PathVariable UUID communityId, @PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Like like = communityService.likePost(communityId, postId, authenticatedUser.getUserId());
        return ResponseEntity.ok(like);
    }

    @PostMapping("/{communityId}/posts/{postId}/comment")
    public ResponseEntity<CommentDTO> commentPost(
            @PathVariable UUID communityId,
            @PathVariable Long postId,
            @Valid @RequestBody CommentDTO commentDTO) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        commentDTO.setUserId(authenticatedUser.getUserId());
        Comment comment = communityService.commentPost(communityId, postId, commentDTO);
        
        CommentDTO responseDto = new CommentDTO();
        responseDto.setCommentId(comment.getCommentId());
        responseDto.setPostId(comment.getPost().getPostId());
        responseDto.setUserId(comment.getUser().getUserId());
        responseDto.setUsername(comment.getUser().getUsername());
        responseDto.setContent(comment.getContent());
        responseDto.setCreatedAt(comment.getCreatedAt());
        return ResponseEntity.ok(responseDto);
    }

    @PostMapping("/{communityId}/posts/{postId}/repost")
    public ResponseEntity<Repost> repostPost(@PathVariable UUID communityId, @PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        Repost repost = communityService.repostPost(communityId, postId, authenticatedUser.getUserId());
        return ResponseEntity.ok(repost);
    }

    @PostMapping("/{communityId}/posts/{postId}/share")
    public ResponseEntity<ShareLinkDTO> sharePost(@PathVariable UUID communityId, @PathVariable Long postId) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        ShareLink shareLink = communityService.sharePost(communityId, postId, authenticatedUser.getUserId());
        
        ShareLinkDTO dto = new ShareLinkDTO();
        dto.setShareId(shareLink.getShareId());
        dto.setPostId(shareLink.getPost().getPostId());
        dto.setUserId(shareLink.getUser().getUserId());
        dto.setShareUrl(shareLink.getShareUrl());
        dto.setCreatedAt(shareLink.getCreatedAt());
        dto.setClickCount(shareLink.getClickCount());
        return ResponseEntity.ok(dto);
    }
    
    private PostDTO convertToPostDTO(Post post) {
        PostDTO dto = new PostDTO();
        dto.setPostId(post.getPostId());
        dto.setContent(post.getContent());
        dto.setTitle(post.getTitle());
        if (post.getUser() != null) {
            dto.setUsername(post.getUser().getUsername());
        }
        if (post.getCommunity() != null) {
            dto.setCommunityName(post.getCommunity().getName());
        }
        dto.setImageUrl(post.getImageUrl());
        dto.setVideoUrl(post.getVideoUrl());
        dto.setCreatedAt(post.getCreatedAt());
        dto.setLikesCount(post.getLikesCount());
        dto.setRepostsCount(post.getRepostsCount());
        dto.setCommentsCount(post.getCommentsCount());
        dto.setSharesCount(post.getSharesCount());
        dto.setViewsCount(post.getViewsCount());
        return dto;
    }
}