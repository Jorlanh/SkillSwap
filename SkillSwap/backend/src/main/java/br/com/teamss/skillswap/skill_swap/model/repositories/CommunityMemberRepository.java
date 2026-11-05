package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.CommunityMember;
import br.com.teamss.skillswap.skill_swap.model.entities.CommunityMemberId; // ADICIONADO
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityMemberRepository extends JpaRepository<CommunityMember, CommunityMemberId> {
    boolean existsById_CommunityIdAndId_UserId(UUID communityId, UUID userId);
    Optional<CommunityMember> findById_CommunityIdAndId_UserId(UUID communityId, UUID userId);
    List<CommunityMember> findById_CommunityIdAndMembershipStatusOrderByJoinedAtAsc(UUID communityId, String status);
    List<CommunityMember> findById_CommunityId(UUID communityId);
    List<CommunityMember> findById_UserIdAndMembershipStatus(UUID userId, String status);
    long countById_CommunityIdAndMembershipStatus(UUID communityId, String status);
}
