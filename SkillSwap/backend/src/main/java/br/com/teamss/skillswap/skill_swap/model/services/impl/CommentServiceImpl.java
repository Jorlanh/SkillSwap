package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommentLikeRepository;
import br.com.teamss.skillswap.skill_swap.model.services.CommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository, CommentLikeRepository commentLikeRepository) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentLikeRepository = commentLikeRepository;
    }

    @Override
    public Comment createComment(Long postId, UUID userId, String content) {
        return createComment(postId, userId, content, null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Comment createComment(Long postId, UUID userId, String content, Long parentCommentId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post não encontrado!"));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Usuário não encontrado!"));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);
        comment.setCreatedAt(Instant.now());
        if (parentCommentId != null) {
            Comment parent = commentRepository.findById(parentCommentId)
                .orElseThrow(() -> new RuntimeException("Comentário principal não encontrado."));
            if (!parent.getPost().getPostId().equals(postId)) throw new IllegalArgumentException("A resposta pertence a outro post.");
            comment.setParentComment(parent);
        }
        post.setCommentsCount(post.getCommentsCount() + 1);
        postRepository.save(post);

        return commentRepository.save(comment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByPost(Long postId) {
        List<Comment> comments = commentRepository.findAllByPost_PostId(postId);
        return comments.stream()
                .map(comment -> {
                    CommentDTO dto = new CommentDTO();
                    dto.setCommentId(comment.getCommentId());
                    dto.setContent(comment.getContent());
                    dto.setCreatedAt(comment.getCreatedAt());
                    dto.setPostId(comment.getPost().getPostId());
                    dto.setUserId(comment.getUser().getUserId());
                    dto.setUsername(comment.getUser().getUsername());
                    dto.setParentCommentId(comment.getParentComment() == null ? null : comment.getParentComment().getCommentId());
                    dto.setLikesCount(commentLikeRepository.countByComment_CommentId(comment.getCommentId()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public void deleteComment(Long commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comentário não encontrado."));
        if (!comment.getUser().getUserId().equals(userId)) throw new org.springframework.security.access.AccessDeniedException("Você só pode excluir seus próprios comentários.");
        Post post = comment.getPost();
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - 1));
        postRepository.save(post);
        commentRepository.delete(comment);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public long toggleLike(Long commentId, UUID userId) {
        Comment comment = commentRepository.findById(commentId).orElseThrow(() -> new RuntimeException("Comentário não encontrado."));
        if (commentLikeRepository.existsByComment_CommentIdAndUser_UserId(commentId, userId)) {
            commentLikeRepository.deleteByComment_CommentIdAndUser_UserId(commentId, userId);
        } else {
            User user = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("Usuário não encontrado."));
            commentLikeRepository.save(new br.com.teamss.skillswap.skill_swap.model.entities.CommentLike(comment, user));
        }
        return commentLikeRepository.countByComment_CommentId(commentId);
    }
}
