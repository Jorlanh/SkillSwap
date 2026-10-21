package br.com.teamss.skillswap.skill_swap.dto;

import jakarta.validation.constraints.NotBlank; // Adicionado
import jakarta.validation.constraints.NotNull; // Adicionado
import jakarta.validation.constraints.Size; // Adicionado
import java.util.UUID;

public class ChatMessageRequestDTO {
    @NotNull(message = "senderId não pode ser nulo") // Adicionado
    private UUID senderId;

    @NotNull(message = "receiverId não pode ser nulo") // Adicionado
    private UUID receiverId;

    @NotBlank(message = "Conteúdo da mensagem não pode ser vazio") // Adicionado
    @Size(max = 2000, message = "Mensagem não pode exceder 2000 caracteres") // Adicionado (ajuste o limite se necessário)
    private String content;

    public UUID getSenderId() {
        return senderId;
    }

    public void setSenderId(UUID senderId) {
        this.senderId = senderId;
    }

    public UUID getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(UUID receiverId) {
        this.receiverId = receiverId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
