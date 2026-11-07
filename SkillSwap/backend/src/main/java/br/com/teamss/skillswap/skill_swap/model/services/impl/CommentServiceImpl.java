package br.com.teamss.skillswap.skill_swap.model.services.impl;

import br.com.teamss.skillswap.skill_swap.dto.CommentDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Comment;
import br.com.teamss.skillswap.skill_swap.model.entities.Post;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.entities.Profile;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommentRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.PostRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.UserRepository;
import br.com.teamss.skillswap.skill_swap.model.repositories.CommentLikeRepository;
import br.com.teamss.skillswap.skill_swap.model.services.CommentService;
import br.com.teamss.skillswap.skill_swap.model.services.ContentModerationService;
import br.com.teamss.skillswap.skill_swap.model.services.NotificationService;
import br.com.teamss.skillswap.skill_swap.model.exception.InappropriateContentException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final ContentModerationService moderationService;
    private final NotificationService notificationService;

    @Autowired
    public CommentServiceImpl(CommentRepository commentRepository, PostRepository postRepository, UserRepository userRepository, CommentLikeRepository commentLikeRepository, ContentModerationService moderationService, NotificationService notificationService) {
        this.commentRepository = commentRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.moderationService = moderationService;
        this.notificationService = notificationService;
    }

    @Override
    public Comment createComment(Long postId, UUID userId, String content) {
        return createComment(postId, userId, content, null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional
    public Comment createComment(Long postId, UUID userId, String content, Long parentCommentId) {
        if (moderationService.isContentInappropriate(content)) {
            throw new InappropriateContentException("O comentário viola as diretrizes de convivência da SkillSwap.");
        }
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
        Comment savedComment = commentRepository.save(comment);

        String preview = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (preview.length() > 140) {
            preview = preview.substring(0, 137) + "...";
        }
        String notificationMessage = "@" + user.getUsername()
                + (parentCommentId == null ? " comentou no seu post: " : " respondeu ao seu comentário: ")
                + preview;
        if (!post.getUser().getUserId().equals(userId)) {
            notificationService.createNotification(post.getUser().getUserId(), notificationMessage);
        }
        if (parentCommentId != null) {
            User parentOwner = savedComment.getParentComment().getUser();
            if (!parentOwner.getUserId().equals(userId)
                    && !parentOwner.getUserId().equals(post.getUser().getUserId())) {
                notificationService.createNotification(parentOwner.getUserId(), notificationMessage);
            }
        }
        return savedComment;
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByPost(Long postId) {
        return getCommentsByPost(postId, null);
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public List<CommentDTO> getCommentsByPost(Long postId, UUID authenticatedUserId) {
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
                    dto.setVerifiedBadge(comment.getUser().isVerifiedBadge());
                    Profile profile = comment.getUser().getProfile();
                    dto.setAvatarUrl(profile == null ? null : profile.getImageUrl());
                    dto.setParentCommentId(comment.getParentComment() == null ? null : comment.getParentComment().getCommentId());
                    dto.setLikesCount(commentLikeRepository.countByComment_CommentId(comment.getCommentId()));
                    dto.setLikedByMe(authenticatedUserId != null
                            && commentLikeRepository.existsByComment_CommentIdAndUser_UserId(comment.getCommentId(), authenticatedUserId));
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
        // Uma resposta fica vinculada ao comentário principal no banco. Ao
        // apagar um comentário principal, o PostgreSQL remove suas respostas
        // em cascata; o contador precisa acompanhar essa mesma quantidade.
        Set<Long> removedIds = new HashSet<>();
        removedIds.add(commentId);
        boolean expanded;
        do {
            expanded = false;
            for (Comment candidate : commentRepository.findAllByPost_PostId(post.getPostId())) {
                Long parentId = candidate.getParentComment() == null ? null : candidate.getParentComment().getCommentId();
                if (parentId != null && removedIds.contains(parentId) && removedIds.add(candidate.getCommentId())) {
                    expanded = true;
                }
            }
        } while (expanded);
        post.setCommentsCount(Math.max(0, post.getCommentsCount() - removedIds.size()));
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
