package br.com.teamss.skillswap.skill_swap.dto;

import lombok.Getter;
import lombok.Setter;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

@Getter
@Setter
public class ReportDTO {
    @NotNull
    private UUID reporterId;
    @NotNull
    private UUID reportedUserId;
    @NotBlank
    @Size(max = 1000)
    private String reason;
    /** Mensagem/post denunciado, quando disponível. Evita classificar apenas o motivo da denúncia. */
    @Size(max = 10000)
    private String reportedContent;
}
