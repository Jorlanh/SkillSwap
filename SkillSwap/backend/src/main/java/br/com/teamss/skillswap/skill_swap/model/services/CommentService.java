package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import java.util.List;
import java.util.UUID;

public interface CommentService {
    Comment createComment(Long postId, UUID userId, String content);
    
    List<CommentDTO> getCommentsByPost(Long postId);
}