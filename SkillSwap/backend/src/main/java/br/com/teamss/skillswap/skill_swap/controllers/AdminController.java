package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.BanRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminRoleRequest;
import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminOperationsReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinLedgerResponse;
import br.com.teamss.skillswap.skill_swap.dto.AdminWalletAdjustmentRequest;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinWalletResponse;
import br.com.teamss.skillswap.skill_swap.dto.ModerationCaseDTO;
import br.com.teamss.skillswap.skill_swap.dto.ModerationDecisionRequest;
import br.com.teamss.skillswap.skill_swap.dto.ModerationPolicyDTO;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

@RestController
@RequestMapping("/api/console-management")
// ALTERAÇÃO PRINCIPAL: Protege todos os endpoints neste controller.
// Apenas usuários com o papel persistido ADMIN podem acessar o console.
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ReportService reportService;

    public AdminController(AdminService adminService, ReportService reportService) {
        this.adminService = adminService;
        this.reportService = reportService;
    }

    @GetMapping("/stats")
    public ResponseEntity<PlatformStatsDTO> getPlatformStats() {
        return ResponseEntity.ok(adminService.getPlatformStatistics());
    }

    @GetMapping("/operations-report")
    public ResponseEntity<AdminOperationsReportDTO> getOperationsReport() {
        return ResponseEntity.ok(adminService.getOperationsReport());
    }

    @GetMapping("/ledger")
    public ResponseEntity<Page<SwapCoinLedgerResponse>> getLedgerAudit(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size) {
        int boundedSize = Math.min(Math.max(size, 1), 200);
        return ResponseEntity.ok(adminService.getLedgerAudit(PageRequest.of(Math.max(page, 0), boundedSize,
            Sort.by("createdAt").descending())));
    }

    @PostMapping("/users/{userId}/wallet-adjustment")
    public ResponseEntity<SwapCoinWalletResponse> adjustWallet(@PathVariable UUID userId,
                                                                @Valid @RequestBody AdminWalletAdjustmentRequest request) {
        return ResponseEntity.ok(adminService.adjustWallet(userId, request));
    }

    @GetMapping("/users")
    public ResponseEntity<List<UserManagementDTO>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @PostMapping("/users/{userId}/toggle-verification")
    public ResponseEntity<UserManagementDTO> toggleVerification(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.toggleUserVerification(userId));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable UUID userId, @RequestBody BanRequestDTO banRequest) {
        adminService.banUser(userId, banRequest.getReason(), banRequest.getExpiresAt(), banRequest.getIpAddress());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/users/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable UUID userId) {
        adminService.unbanUser(userId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<UserManagementDTO> setRole(@PathVariable UUID userId, @Valid @RequestBody AdminRoleRequest request) {
        return ResponseEntity.ok(adminService.setRole(userId, request.role(), request.enabled()));
    }

    @GetMapping("/moderation/cases")
    public ResponseEntity<List<ModerationCaseDTO>> getModerationCases() {
        return ResponseEntity.ok(reportService.getModerationQueue());
    }

    @GetMapping("/moderation/policy")
    public ResponseEntity<ModerationPolicyDTO> getModerationPolicy() {
        return ResponseEntity.ok(reportService.getModerationPolicy());
    }

    @PutMapping("/moderation/policy")
    public ResponseEntity<ModerationPolicyDTO> updateModerationPolicy(
            @Valid @RequestBody ModerationPolicyDTO policy) {
        return ResponseEntity.ok(reportService.updateModerationPolicy(policy));
    }

    @PostMapping("/moderation/cases/{reportId}/decision")
    public ResponseEntity<ModerationCaseDTO> decideModeration(
            @PathVariable Long reportId,
            @Valid @RequestBody ModerationDecisionRequest request) {
        return ResponseEntity.ok(reportService.decide(reportId, request));
    }
}
