package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "tb_chat_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private String content;

    @Lob
    @Column(name = "voice_data", columnDefinition = "bytea") // Adicionado columnDefinition
    private byte[] voiceData;

    @Lob
    @Column(name = "file_data", columnDefinition = "bytea") // Adicionado columnDefinition
    private byte[] fileData;

    @Column(name = "file_type")
    private String fileType;

    @CreationTimestamp
    private Instant sentAt;
}