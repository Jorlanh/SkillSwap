package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Like;
import br.com.teamss.skillswap.skill_swap.model.entities.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.UUID;

@Repository
public interface LikeRepository extends JpaRepository<Like, LikeId> {
    boolean existsByPost_PostIdAndUser_UserId(Long postId, UUID userId);
    
    List<Like> findAllByPost_PostId(Long postId);
}