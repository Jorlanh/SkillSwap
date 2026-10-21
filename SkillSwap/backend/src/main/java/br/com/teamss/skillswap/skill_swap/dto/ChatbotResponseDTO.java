package br.com.teamss.skillswap.skill_swap.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotResponseDTO {

    private String response;

    // Optional: Context to be sent back in the next request for stateful chat
    private String context;

    // Optional: Indicates if the response was simplified
    private boolean wasSimplified = false;
}
