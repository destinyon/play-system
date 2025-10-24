package com.example.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ChatSessionSummary {
    private String sessionId;
    private Integer orderId;
    private Integer peerId;
    private String peerName;
    private String peerRole;
    private String peerAvatar;
    private String orderNo;
    private String orderStatus;
    private String orderRemark;
    private String lastMessage;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
}
