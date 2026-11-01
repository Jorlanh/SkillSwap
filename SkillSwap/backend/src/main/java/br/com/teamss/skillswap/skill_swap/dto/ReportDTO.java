package br.com.teamss.skillswap.skill_swap.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class ReportDTO {
    private UUID reporterId;
    private UUID reportedUserId;
    private String reason;
}