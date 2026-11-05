package br.com.teamss.skillswap.skill_swap.dto;

import java.time.Instant;
import java.util.UUID;

/** Representacao segura de uma mensagem, sem serializar a entidade User inteira. */
public class ChatMessageResponseDTO {
    private Long messageId;
    private UUID senderId;
    private UUID receiverId;
    private String content;
    private byte[] voiceData;
    private byte[] fileData;
    private String fileType;
    private Instant sentAt;

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public UUID getSenderId() { return senderId; }
    public void setSenderId(UUID senderId) { this.senderId = senderId; }
    public UUID getReceiverId() { return receiverId; }
    public void setReceiverId(UUID receiverId) { this.receiverId = receiverId; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public byte[] getVoiceData() { return voiceData; }
    public void setVoiceData(byte[] voiceData) { this.voiceData = voiceData; }
    public byte[] getFileData() { return fileData; }
    public void setFileData(byte[] fileData) { this.fileData = fileData; }
    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }
    public Instant getSentAt() { return sentAt; }
    public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
