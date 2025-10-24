package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.ChatMessageDelivery;

public interface ChatService {
    Result listSessions(DataRequest request);

    Result loadHistory(DataRequest request);

    Result markRead(DataRequest request);

    Result sendMessage(DataRequest request);

    ChatMessageDelivery dispatchMessage(DataRequest request);
}
