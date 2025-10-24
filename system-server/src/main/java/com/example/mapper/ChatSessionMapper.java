package com.example.mapper;

import com.example.entity.ChatSession;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ChatSessionMapper {

    int insert(ChatSession session);

    ChatSession findByKey(@Param("sessionKey") String sessionKey);

    ChatSession findById(@Param("id") Integer id);

    List<ChatSession> listByRestaurateur(@Param("restaurateurId") Integer restaurateurId);

    List<ChatSession> listByPeer(@Param("peerUserId") Integer peerUserId,
                                 @Param("peerRole") String peerRole);

    int updateLastMessage(@Param("sessionId") Integer sessionId,
                          @Param("preview") String preview,
                          @Param("time") LocalDateTime time);

    int updateTitle(@Param("sessionId") Integer sessionId,
                    @Param("title") String title,
                    @Param("contextType") String contextType,
                    @Param("contextRef") Integer contextRef,
                    @Param("orderId") Integer orderId);
}
