package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminOperationsReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinLedgerResponse;
import br.com.teamss.skillswap.skill_swap.dto.AdminWalletAdjustmentRequest;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinWalletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface AdminService {
    List<UserManagementDTO> getAllUsers();
    UserManagementDTO toggleUserVerification(UUID userId);
    PlatformStatsDTO getPlatformStatistics();

    // Novos métodos
    void banUser(UUID userId, String reason, Instant expiresAt, String ipAddress);
    void unbanUser(UUID userId);
    UserManagementDTO setRole(UUID userId, String roleName, boolean enabled);
    AdminOperationsReportDTO getOperationsReport();
    Page<SwapCoinLedgerResponse> getLedgerAudit(Pageable pageable);
    SwapCoinWalletResponse adjustWallet(UUID userId, AdminWalletAdjustmentRequest request);
}
