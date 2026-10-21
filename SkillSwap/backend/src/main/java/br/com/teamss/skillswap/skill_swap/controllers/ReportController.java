package br.com.teamss.skillswap.skill_swap.controllers;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.ModerationAppealRequest;
import br.com.teamss.skillswap.skill_swap.dto.ModerationCaseDTO;
import br.com.teamss.skillswap.skill_swap.model.services.ReportService;
import br.com.teamss.skillswap.skill_swap.model.services.UserServiceDTO;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Objects;

@RestController
@RequestMapping("/api/reports")
@PreAuthorize("isAuthenticated()")
public class ReportController {

    private final ReportService reportService;
    private final UserServiceDTO userServiceDTO;

    public ReportController(ReportService reportService, UserServiceDTO userServiceDTO) {
        this.reportService = reportService;
        this.userServiceDTO = userServiceDTO;
    }

    @PostMapping
    public ResponseEntity<ModerationCaseDTO> createReport(@Valid @RequestBody ReportDTO reportDTO) {
        if (!Objects.equals(userServiceDTO.getAuthenticatedUser().getUserId(), reportDTO.getReporterId())) {
            throw new AccessDeniedException("O denunciante deve ser a conta autenticada.");
        }
        return ResponseEntity.ok(ModerationCaseDTO.from(reportService.createReport(reportDTO)));
    }

    @PostMapping("/{reportId}/appeal")
    public ResponseEntity<ModerationCaseDTO> appeal(@PathVariable Long reportId,
                                                     @Valid @RequestBody ModerationAppealRequest request) {
        return ResponseEntity.ok(reportService.submitAppeal(reportId, request));
    }
}
