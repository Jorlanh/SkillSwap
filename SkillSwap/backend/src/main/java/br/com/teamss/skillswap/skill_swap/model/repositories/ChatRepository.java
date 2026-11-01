package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.entities.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatRepository extends JpaRepository<ChatMessage, Long> {

    // Busca o histórico entre dois usuários específicos (Bidirecional)
    @Query("SELECT cm FROM ChatMessage cm WHERE " +
           "(cm.sender.userId = :id1 AND cm.receiver.userId = :id2) OR " +
           "(cm.sender.userId = :id2 AND cm.receiver.userId = :id1) " +
           "ORDER BY cm.sentAt ASC")
    List<ChatMessage> findBySenderIdAndReceiverIdOrSenderIdAndReceiverId(
            @Param("id1") UUID id1, @Param("id2") UUID id2, 
            @Param("id2_alt") UUID id2_alt, @Param("id1_alt") UUID id1_alt);

    // Busca TODAS as mensagens de um usuário para compor a lista de conversas ativas
    @Query("SELECT m FROM ChatMessage m WHERE m.sender.userId = :userId OR m.receiver.userId = :userId ORDER BY m.sentAt DESC")
    List<ChatMessage> findAllBySenderIdOrReceiverId(@Param("userId") UUID userId);
}