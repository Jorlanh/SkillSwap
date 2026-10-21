package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank; // Adicionado
import jakarta.validation.constraints.NotNull; // Adicionado
import jakarta.validation.constraints.Size; // Adicionado
import java.time.Instant;
import java.util.UUID;

public class CommentDTO {
    private Long commentId; // Gerado pelo backend

    @NotNull(message = "postId não pode ser nulo") // Adicionado
    private Long postId;

    @NotNull(message = "userId não pode ser nulo") // Adicionado
    private UUID userId;

    private String username; // Pode ser preenchido pelo backend

    @NotBlank(message = "Conteúdo do comentário não pode ser vazio") // Adicionado
    @Size(max = 1000, message = "Comentário não pode exceder 1000 caracteres") // Adicionado
    private String content;

    private Instant createdAt; // Gerado pelo backend

    // Getters e Setters
    public Long getCommentId() { return commentId; } // Corrigido para retornar commentId
    public void setCommentId(Long commentId) { this.commentId = commentId; }
    public Long getPostId() { return postId; }
    public void setPostId(Long postId) { this.postId = postId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
