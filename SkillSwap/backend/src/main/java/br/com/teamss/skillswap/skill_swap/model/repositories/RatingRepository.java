package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Rating;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RatingRepository extends JpaRepository<Rating, Long> {

    boolean existsByProposal_ProposalIdAndRaterUser_UserId(Long proposalId, UUID raterId);
    boolean existsByRatedUser_UserIdAndStars(UUID userId, Integer stars);

    @Query("SELECT r.ratedUser.id, AVG(r.stars), COUNT(r.id) " +
           "FROM Rating r " +
           "WHERE r.proposal.offeredSkill.id = :skillId OR r.proposal.requestedSkill.id = :skillId " +
           "GROUP BY r.ratedUser.id " +
           "ORDER BY AVG(r.stars) DESC, COUNT(r.id) DESC")
    List<Object[]> findUserRankingsBySkill(@Param("skillId") Long skillId);

    @Query("SELECT r.ratedUser.id, AVG(r.stars), COUNT(r.id) " +
           "FROM Rating r " +
           "GROUP BY r.ratedUser.id " +
           "ORDER BY AVG(r.stars) DESC, COUNT(r.id) DESC")
    List<Object[]> findGeneralUserRankings();
}
