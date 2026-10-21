package br.com.teamss.skillswap.skill_swap.model.repositories;

import br.com.teamss.skillswap.skill_swap.model.wallet.SwapCoinPayment;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import br.com.teamss.skillswap.skill_swap.model.wallet.PaymentStatus;

public interface SwapCoinPaymentRepository extends JpaRepository<SwapCoinPayment, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select payment from SwapCoinPayment payment where payment.stripeCheckoutSessionId = :sessionId")
    Optional<SwapCoinPayment> findByStripeCheckoutSessionIdForUpdate(@Param("sessionId") String sessionId);

    Optional<SwapCoinPayment> findByPaymentIdAndUser_UserId(UUID paymentId, UUID userId);
    long countByStatus(PaymentStatus status);

    @Query("select coalesce(sum(p.coinAmount), 0) from SwapCoinPayment p where p.status = :status")
    long sumCoinAmountByStatus(@Param("status") PaymentStatus status);

    @Query("select coalesce(sum(p.amountCents), 0) from SwapCoinPayment p where p.status = :status")
    long sumAmountCentsByStatus(@Param("status") PaymentStatus status);
}
