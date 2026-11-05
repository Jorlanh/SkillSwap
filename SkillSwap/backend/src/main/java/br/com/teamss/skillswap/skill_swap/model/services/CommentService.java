package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import java.util.List;
import java.util.UUID;

public interface CommentService {
    Comment createComment(Long postId, UUID userId, String content);
    Comment createComment(Long postId, UUID userId, String content, Long parentCommentId);
    
    List<CommentDTO> getCommentsByPost(Long postId);
    List<CommentDTO> getCommentsByPost(Long postId, UUID authenticatedUserId);
    void deleteComment(Long commentId, UUID userId);
    long toggleLike(Long commentId, UUID userId);
}
