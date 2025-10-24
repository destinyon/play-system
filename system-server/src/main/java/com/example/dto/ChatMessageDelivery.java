package com.example.dto;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class ChatMessageDelivery {
    ChatMessageDto message;
    ChatSessionSummary senderSession;
    ChatSessionSummary receiverSession;
    String receiverUsername;
    String senderUsername;
}
