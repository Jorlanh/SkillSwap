package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.CommentLike;
import br.com.teamss.skillswap.skill_swap.model.entities.CommentLikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CommentLikeRepository extends JpaRepository<CommentLike, CommentLikeId> {
    boolean existsByComment_CommentIdAndUser_UserId(Long commentId, UUID userId);
    long countByComment_CommentId(Long commentId);
    void deleteByComment_CommentIdAndUser_UserId(Long commentId, UUID userId);
}
