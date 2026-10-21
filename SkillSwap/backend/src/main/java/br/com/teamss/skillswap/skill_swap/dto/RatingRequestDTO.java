package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RatingRequestDTO {
    @NotNull(message = "O ID da proposta é obrigatório.")
    private Long proposalId;

    @NotNull(message = "A pontuação em estrelas é obrigatória.")
    @Min(value = 1, message = "A pontuação mínima é 1 estrela.")
    @Max(value = 5, message = "A pontuação máxima é 5 estrelas.")
    private Integer stars;

    @Size(max = 500, message = "O comentário não pode exceder 500 caracteres.")
    private String comment;
}