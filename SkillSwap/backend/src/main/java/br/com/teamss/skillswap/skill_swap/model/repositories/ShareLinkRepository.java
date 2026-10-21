package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.ShareLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ShareLinkRepository extends JpaRepository<ShareLink, Long> {
    Optional<ShareLink> findByShareUrl(String shareUrl);

    // ADICIONADO: Busca ShareLinks por lista de postIds
    List<ShareLink> findAllByPost_PostIdIn(List<Long> postIds);
}