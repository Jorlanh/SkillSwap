package br.com.teamss.skillswap.skill_swap.model.services;

import br.com.teamss.skillswap.skill_swap.dto.ReportDTO;
import br.com.teamss.skillswap.skill_swap.dto.ModerationAppealRequest;
import br.com.teamss.skillswap.skill_swap.dto.ModerationCaseDTO;
import br.com.teamss.skillswap.skill_swap.dto.ModerationDecisionRequest;
import br.com.teamss.skillswap.skill_swap.dto.ModerationPolicyDTO;
import br.com.teamss.skillswap.skill_swap.model.entities.Report;
import br.com.teamss.skillswap.skill_swap.model.entities.User;
import br.com.teamss.skillswap.skill_swap.model.services.ModerationAssessment;
import java.util.List;

public interface ReportService {
    Report createReport(ReportDTO reportDTO);
    Report createAutomatedCase(User reportedUser, String content, String context, ModerationAssessment assessment);
    List<Report> getPendingReports();
    List<ModerationCaseDTO> getModerationQueue();
    ModerationPolicyDTO getModerationPolicy();
    ModerationPolicyDTO updateModerationPolicy(ModerationPolicyDTO policy);
    ModerationCaseDTO decide(Long reportId, ModerationDecisionRequest request);
    ModerationCaseDTO submitAppeal(Long reportId, ModerationAppealRequest request);
    void processReports();
}
