package br.com.teamss.skillswap.skill_swap.model.wallet;

import br.com.teamss.skillswap.skill_swap.model.entities.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "swapcoin_wallets", uniqueConstraints = @UniqueConstraint(name = "uk_swapcoin_wallet_user", columnNames = "user_id"))
public class SwapCoinWallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID walletId;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User owner;

    @Column(name = "balance", nullable = false)
    private long balance;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    public UUID getWalletId() { return walletId; }
    public User getOwner() { return owner; }
    public void setOwner(User owner) { this.owner = owner; }
    public long getBalance() { return balance; }
    public long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void credit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("O crédito deve ser positivo.");
        balance = Math.addExact(balance, amount);
        updatedAt = Instant.now();
    }

    public void debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("O débito deve ser positivo.");
        if (balance < amount) throw new IllegalStateException("Saldo de SwapCoins insuficiente.");
        balance -= amount;
        updatedAt = Instant.now();
    }
}
