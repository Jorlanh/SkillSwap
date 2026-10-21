package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Repost;
import br.com.teamss.skillswap.skill_swap.model.entities.RepostId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface RepostRepository extends JpaRepository<Repost, RepostId> {
    boolean existsByPost_PostIdAndUser_UserId(Long postId, UUID userId); // ADICIONADO
}