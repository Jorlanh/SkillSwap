package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "tb_permissions")
public class Permission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "permission_id")
    private Long permissionId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "permission_type", nullable = false)
    private String permissionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PermissionStatus status;

    @Column(name = "created_at")
    private Instant  createdAt;

    @Column(name = "updated_at")
    private Instant  updatedAt;

    // Construtores
    public Permission() {}

    public Permission(UUID userId, String permissionType, PermissionStatus status) {
        this.userId = userId;
        this.permissionType = permissionType;
        this.status = status;
    }

    // Getters e Setters
    public Long getPermissionId() { return permissionId; }
    public void setPermissionId(Long permissionId) { this.permissionId = permissionId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public String getPermissionType() { return permissionType; }
    public void setPermissionType(String permissionType) { this.permissionType = permissionType; }
    public PermissionStatus getStatus() { return status; }
    public void setStatus(PermissionStatus status) { this.status = status; }
    public Instant  getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant  createdAt) { this.createdAt = createdAt; }
    public Instant  getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant  updatedAt) { this.updatedAt = updatedAt; }
}