package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.CommunityMember;
import br.com.teamss.skillswap.skill_swap.model.entities.CommunityMemberId; // ADICIONADO
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, CommunityMemberId> {
    boolean existsById_CommunityIdAndId_UserId(UUID communityId, UUID userId);
}