package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ChatbotRequestDTO {

    @NotBlank(message = "A mensagem não pode estar vazia.")
    @Size(max = 1000, message = "A mensagem não pode exceder 1000 caracteres.")
    private String message;

    // Optional: Context from previous conversation turn, if needed for stateful chat
    private String context;

    // Optional: Flag to request text simplification
    private boolean simplifyResponse = false;
}
