package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.AdminReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.UserDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.ReportStatus;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportService reportService;
    private final UserServiceDTO userServiceDTO;

    public ReportController(ReportService reportService, UserServiceDTO userServiceDTO) {
        this.reportService = reportService;
        this.userServiceDTO = userServiceDTO;
    }

    @PostMapping
    public ResponseEntity<Void> createReport(@RequestBody ReportDTO reportDTO) {
        UserDTO authenticatedUser = userServiceDTO.getAuthenticatedUser();
        // O autor da denúncia vem do token autenticado, nunca do JSON enviado
        // pelo navegador. Isso impede que alguém registre denúncias em nome de
        // outra conta.
        reportDTO.setReporterId(authenticatedUser.getUserId());
        reportService.createReport(reportDTO);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/pending")
    @PreAuthorize("@platformAdminAuthorization.isAdmin(authentication)")
    public ResponseEntity<List<AdminReportDTO>> pendingReports() {
        return ResponseEntity.ok(reportService.getPendingReports().stream().map(report ->
                new AdminReportDTO(report.getId(), report.getReporter().getUserId(), report.getReporter().getUsername(),
                        report.getReportedUser().getUserId(), report.getReportedUser().getUsername(), report.getReason(),
                        report.getStatus().name(), report.getCreatedAt(), report.getTargetType(), report.getTargetId(), report.getCategory())).toList());
    }

    @PostMapping("/{reportId}/review")
    @PreAuthorize("@platformAdminAuthorization.isAdmin(authentication)")
    public ResponseEntity<Void> reviewReport(@PathVariable Long reportId,
                                             @RequestParam ReportStatus status,
                                             @RequestParam(defaultValue = "") String responseMessage) {
        reportService.reviewReport(reportId, status, responseMessage);
        return ResponseEntity.noContent().build();
    }
}
