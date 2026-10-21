package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_bans")
@Getter
@Setter
@NoArgsConstructor
public class Ban {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(nullable = false)
    private String reason;

    @Column(name = "banned_by")
    private UUID bannedBy; // ID do administrador que baniu

    @Column(name = "expires_at")
    private Instant expiresAt; // Nulo para banimentos permanentes

    @CreationTimestamp
    private Instant createdAt;

    private boolean active = true;
}