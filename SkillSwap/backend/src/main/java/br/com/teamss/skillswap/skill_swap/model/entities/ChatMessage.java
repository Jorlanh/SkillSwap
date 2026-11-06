package br.com.teamss.skillswap.skill_swap.model.entities;

import jakarta.persistence.*;
import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

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
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "message_id")
    private Long messageId;

    @ManyToOne
    @JoinColumn(name = "sender_id")
    private User sender;

    @ManyToOne
    @JoinColumn(name = "receiver_id")
    private User receiver;

    private String content;

    // PostgreSQL BYTEA must not be mapped as a JDBC LOB/OID. Using VARBINARY
    // keeps the Java byte[] compatible with the schema created by Flyway.
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "voice_data", columnDefinition = "bytea")
    private byte[] voiceData;

    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(name = "file_data", columnDefinition = "bytea")
    private byte[] fileData;

    @Column(name = "file_type")
    private String fileType;

    @Column(name = "voice_type")
    private String voiceType;

    @CreationTimestamp
    private Instant sentAt;
}
