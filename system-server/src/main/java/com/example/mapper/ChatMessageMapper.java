package com.example.mapper;

import com.example.entity.ChatMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    int insert(ChatMessage message);

    ChatMessage selectLatest(@Param("sessionId") Integer sessionId);

    Integer countUnread(@Param("sessionId") Integer sessionId,
                        @Param("receiverRole") String receiverRole,
                        @Param("receiverId") Integer receiverId);

    List<ChatMessage> listBySession(@Param("sessionId") Integer sessionId,
                                    @Param("limit") Integer limit,
                                    @Param("beforeId") Integer beforeId);

    int markRead(@Param("sessionId") Integer sessionId,
                 @Param("receiverRole") String receiverRole,
                 @Param("receiverId") Integer receiverId);
}
