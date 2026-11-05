package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProposalRepository extends JpaRepository<Proposal, Long> {
    
    @Query("SELECT p FROM Proposal p WHERE p.sender.userId = :userId OR p.receiver.userId = :userId")
    List<Proposal> findBySenderIdOrReceiverId(@Param("userId") UUID userId);

    // MÉTODO A ADICIONAR
    @Query("SELECT COUNT(p) FROM Proposal p WHERE p.status = :status AND (p.sender.userId = :userId OR p.receiver.userId = :userId)")
    long countByStatusAndParticipant(@Param("status") String status, @Param("userId") UUID userId);

    long countByStatus(String status);

    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Proposal p " +
           "WHERE p.status IN ('ACCEPTED', 'COMPLETED') AND " +
           "((p.sender.userId = :userId1 AND p.receiver.userId = :userId2) OR " +
           "(p.sender.userId = :userId2 AND p.receiver.userId = :userId1))")
    boolean existsAcceptedBetween(@Param("userId1") UUID userId1, @Param("userId2") UUID userId2);
}
