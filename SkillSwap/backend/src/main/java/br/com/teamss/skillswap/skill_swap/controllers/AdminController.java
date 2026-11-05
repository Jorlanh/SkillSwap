package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.BanRequestDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminBanDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminRoleRequest;
import br.com.teamss.skillswap.skill_swap.dto.PlatformStatsDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserManagementDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminOperationsReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinLedgerResponse;
import br.com.teamss.skillswap.skill_swap.dto.AdminWalletAdjustmentRequest;
import br.com.teamss.skillswap.skill_swap.dto.SwapCoinWalletResponse;
import br.com.teamss.skillswap.skill_swap.dto.CommunityMemberDTO;
import br.com.teamss.skillswap.skill_swap.dto.CommunitySummaryDTO;
import br.com.teamss.skillswap.skill_swap.model.services.AdminService;
import br.com.teamss.skillswap.skill_swap.model.services.CommunityService;
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
@PreAuthorize("@platformAdminAuthorization.isAdmin(authentication)")
public class AdminController {

    private final AdminService adminService;
    private final CommunityService communityService;

    public AdminController(AdminService adminService, CommunityService communityService) {
        this.adminService = adminService;
        this.communityService = communityService;
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

    @GetMapping("/bans")
    public ResponseEntity<List<AdminBanDTO>> getActiveBans() {
        return ResponseEntity.ok(adminService.getActiveBans());
    }

    @PostMapping("/users/{userId}/toggle-verification")
    public ResponseEntity<UserManagementDTO> toggleVerification(@PathVariable UUID userId) {
        return ResponseEntity.ok(adminService.toggleUserVerification(userId));
    }

    @PostMapping("/users/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable UUID userId, @RequestBody BanRequestDTO banRequest) {
        adminService.banUser(userId, banRequest.getReason(), banRequest.getExpiresAt(), banRequest.getIpAddress(), banRequest.getScope());
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

    @GetMapping("/communities")
    public ResponseEntity<List<CommunitySummaryDTO>> getCommunities() {
        return ResponseEntity.ok(communityService.getCommunities(500));
    }

    @GetMapping("/communities/{communityId}/members")
    public ResponseEntity<List<CommunityMemberDTO>> getCommunityMembers(@PathVariable UUID communityId,
                                                                          @RequestParam(defaultValue = "") String search) {
        return ResponseEntity.ok(communityService.listMembersAsPlatformAdmin(communityId, search));
    }

    @PutMapping("/communities/{communityId}/owner/{newOwnerId}")
    public ResponseEntity<Void> transferCommunityOwnership(@PathVariable UUID communityId,
                                                            @PathVariable UUID newOwnerId) {
        communityService.transferOwnershipAsPlatformAdmin(communityId, newOwnerId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/communities/{communityId}")
    public ResponseEntity<Void> deleteCommunity(@PathVariable UUID communityId) {
        communityService.deleteCommunityAsPlatformAdmin(communityId);
        return ResponseEntity.noContent().build();
    }
}
