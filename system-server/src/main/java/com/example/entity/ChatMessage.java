package com.example.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "chat_message")
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "session_id", nullable = false)
    private Integer sessionId;

    @Column(name = "order_id")
    private Integer orderId;

    @Column(name = "context_type", length = 32)
    private String contextType;

    @Column(name = "context_ref")
    private Integer contextRef;

    @Column(name = "sender_id", nullable = false)
    private Integer senderId;

    @Column(name = "sender_role", nullable = false, length = 32)
    private String senderRole;

    @Column(name = "receiver_id", nullable = false)
    private Integer receiverId;

    @Column(name = "receiver_role", nullable = false, length = 32)
    private String receiverRole;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "read_flag")
    private Boolean readFlag;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}
