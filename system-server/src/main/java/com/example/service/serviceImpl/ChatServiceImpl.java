package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.ChatMessageDelivery;
import com.example.dto.ChatMessageDto;
import com.example.dto.ChatSessionSummary;
import com.example.entity.ChatMessage;
import com.example.entity.ChatSession;
import com.example.entity.OrderInfo;
import com.example.entity.Restaurateur;
import com.example.entity.User;
import com.example.mapper.ChatMessageMapper;
import com.example.mapper.ChatSessionMapper;
import com.example.mapper.OrderMapper;
import com.example.mapper.RestaurateurMapper;
import com.example.mapper.UserMapper;
import com.example.service.ChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final String ROLE_RESTAURATEUR = "RESTAURATEUR";
    private static final String ROLE_DELIVERYMAN = "DELIVERYMAN";
    private static final String ROLE_GUEST = "GUEST";
    private static final int PREVIEW_MAX = 80;

    private final OrderMapper orderMapper;
    private final UserMapper userMapper;
    private final RestaurateurMapper restaurateurMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Result listSessions(DataRequest request) {
        Map<String, Object> data = safeData(request);
        String username = stringValue(data.get("username"));
        if (username == null) {
            return Result.error("缺少用户名");
        }

        User user = userMapper.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (user.getRole() == null) {
            return Result.error("账号缺少角色信息");
        }

        return switch (user.getRole()) {
            case RESTAURATEUR -> Result.success(listSessionsForRestaurateur(user));
            case GUEST -> Result.success(listSessionsForPeer(user, ROLE_GUEST));
            case DELIVERYMAN -> Result.success(listSessionsForPeer(user, ROLE_DELIVERYMAN));
            default -> Result.success(Collections.emptyList());
        };
    }

    private List<ChatSessionSummary> listSessionsForRestaurateur(User user) {
        Restaurateur restaurateur = restaurateurMapper.getByUserId(user.getId());
        if (restaurateur == null) {
            return Collections.emptyList();
        }

        List<OrderInfo> orders = orderMapper.listByRestaurateur(restaurateur.getId());
        if (orders != null) {
            for (OrderInfo order : orders) {
                if (order.getUserId() != null) {
                    User guest = userMapper.getById(order.getUserId());
                    if (guest != null) {
                        ensureSession(restaurateur.getId(), order, ROLE_GUEST, guest);
                    }
                }
                if (order.getDeliverymanId() != null) {
                    User courier = userMapper.getById(order.getDeliverymanId());
                    if (courier != null) {
                        ensureSession(restaurateur.getId(), order, ROLE_DELIVERYMAN, courier);
                    }
                }
            }
        }

        List<ChatSession> sessions = chatSessionMapper.listByRestaurateur(restaurateur.getId());
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        Integer restaurateurUserId = restaurateurMapper.getUserIdByRestaurateurId(restaurateur.getId());
        return mapSessionsForViewer(ROLE_RESTAURATEUR, restaurateurUserId, sessions);
    }

    private List<ChatSessionSummary> listSessionsForPeer(User user, String peerRole) {
        List<OrderInfo> orders;
        if (ROLE_GUEST.equals(peerRole)) {
            orders = orderMapper.listByGuest(user.getId());
        } else if (ROLE_DELIVERYMAN.equals(peerRole)) {
            orders = orderMapper.listByDeliveryman(user.getId());
        } else {
            orders = Collections.emptyList();
        }
        if (orders != null) {
            for (OrderInfo order : orders) {
                if (order.getRestaurateurId() != null) {
                    ensureSession(order.getRestaurateurId(), order, peerRole, user);
                }
            }
        }

        List<ChatSession> sessions = chatSessionMapper.listByPeer(user.getId(), peerRole);
        if (sessions == null || sessions.isEmpty()) {
            return Collections.emptyList();
        }
        return mapSessionsForViewer(peerRole, user.getId(), sessions);
    }

    private List<ChatSessionSummary> mapSessionsForViewer(String viewerRole, Integer receiverId, List<ChatSession> sessions) {
        Map<Integer, OrderInfo> orderCache = new HashMap<>();
        Map<Integer, User> userCache = new HashMap<>();
        Map<Integer, Integer> restaurateurUserIdCache = new HashMap<>();
        return sessions.stream()
            .map(session -> buildSummaryForViewer(session, viewerRole, receiverId, orderCache, userCache, restaurateurUserIdCache))
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing((ChatSessionSummary s) ->
                s.getLastMessageTime() == null ? LocalDateTime.MIN : s.getLastMessageTime()).reversed())
            .collect(Collectors.toList());
    }

    @Override
    public Result loadHistory(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = intValue(data.get("orderId"));
        Integer peerId = intValue(data.get("peerId"));
        String peerRole = stringValue(data.get("peerRole"));
        String username = stringValue(data.get("username"));
        if (orderId == null || username == null) {
            return Result.error("缺少必要参数");
        }

        User viewer = userMapper.getByUsername(username);
        if (viewer == null || viewer.getRole() == null) {
            return Result.error("用户不存在");
        }

        ChatSession session;
        if (ROLE_RESTAURATEUR.equals(viewer.getRole().name())) {
            if (peerId == null || peerRole == null) {
                return Result.error("缺少对端信息");
            }
            Restaurateur restaurateur = restaurateurMapper.getByUserId(viewer.getId());
            if (restaurateur == null) {
                return Result.error("商家资料不完整");
            }
            session = ensureSession(restaurateur.getId(), orderId, peerRole, peerId);
        } else {
            OrderInfo order = orderMapper.getById(orderId);
            if (order == null || order.getRestaurateurId() == null) {
                return Result.error("订单不存在或商家缺失");
            }
            session = ensureSession(order.getRestaurateurId(), order, viewer.getRole().name(), viewer);
        }
        if (session == null) {
            return Result.error("会话不存在");
        }

        Integer limit = request != null && request.getSize() != null ? request.getSize() : 50;
        Integer beforeId = intValue(data.get("beforeId"));
        List<ChatMessage> records = chatMessageMapper.listBySession(session.getId(), limit, beforeId);
        if (records == null) {
            records = Collections.emptyList();
        }

        List<ChatMessageDto> dtos = records.stream()
            .sorted(Comparator.comparing(ChatMessage::getId))
            .map(this::mapToDto)
            .collect(Collectors.toList());

        String sessionKey = session.getSessionKey();
        for (ChatMessageDto dto : dtos) {
            dto.setSessionId(sessionKey);
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("messages", dtos);

        if (session.getOrderId() != null) {
            OrderInfo order = orderMapper.getById(session.getOrderId());
            if (order != null) {
                payload.put("order", orderSummary(order));
            }
        }

        return Result.success(payload);
    }

    @Override
    @Transactional
    public Result markRead(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = intValue(data.get("orderId"));
        Integer peerId = intValue(data.get("peerId"));
        String peerRole = stringValue(data.get("peerRole"));
        String username = stringValue(data.get("username"));
        if (orderId == null || username == null) {
            return Result.error("缺少必要参数");
        }

        User viewer = userMapper.getByUsername(username);
        if (viewer == null || viewer.getRole() == null) {
            return Result.error("用户不存在");
        }
        OrderInfo order = orderMapper.getById(orderId);
        if (order == null || order.getRestaurateurId() == null) {
            return Result.error("订单不存在或商家缺失");
        }
        ChatSession session;
        String viewerRole = viewer.getRole().name();
        if (ROLE_RESTAURATEUR.equals(viewerRole)) {
            // 商家端需要明确对端是谁（顾客/骑手），以命中正确的统一会话键
            if (peerId == null || peerRole == null) {
                return Result.error("缺少对端信息");
            }
            User peer = userMapper.getById(peerId);
            if (peer == null) {
                return Result.error("对端用户不存在");
            }
            session = ensureSession(order.getRestaurateurId(), order, peerRole, peer);
        } else {
            // 顾客/骑手端：对端恒为商家，统一会话以自身身份作为 peer
            session = ensureSession(order.getRestaurateurId(), order, viewerRole, viewer);
        }
        if (session != null) {
            String receiverRole = viewerRole;
            Integer receiverId = ROLE_RESTAURATEUR.equals(receiverRole)
                ? restaurateurMapper.getUserIdByRestaurateurId(order.getRestaurateurId())
                : viewer.getId();
            chatMessageMapper.markRead(session.getId(), receiverRole, receiverId);
        }
        return Result.success();
    }

    @Override
    @Transactional
    public Result sendMessage(DataRequest request) {
        try {
            ChatMessageDelivery delivery = doSendMessage(request);
            return Result.success(delivery.getMessage());
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public ChatMessageDelivery dispatchMessage(DataRequest request) {
        return doSendMessage(request);
    }

    private ChatMessageDelivery doSendMessage(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = intValue(data.get("orderId"));
        Integer receiverId = intValue(data.get("receiverId"));
        String receiverRole = stringValue(data.get("receiverRole"));
        if (receiverRole != null) {
            receiverRole = receiverRole.trim().toUpperCase();
        }
        String content = stringValue(data.get("content"));
        String username = stringValue(data.get("username"));

        if (content != null) {
            content = content.trim();
        }

        if (orderId == null || receiverId == null || receiverRole == null || content == null || content.isEmpty()) {
            throw new IllegalArgumentException("缺少必要参数");
        }
        if (username == null) {
            throw new IllegalArgumentException("请先登录");
        }

        User sender = userMapper.getByUsername(username);
        if (sender == null) {
            throw new IllegalArgumentException("发送人不存在");
        }

        OrderInfo order = orderMapper.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }

        String senderRole = sender.getRole() == null ? null : sender.getRole().name();
        if (senderRole == null) {
            throw new IllegalArgumentException("无法识别发送人角色");
        }

    Restaurateur restaurateur;
    Integer restaurateurId;
    String peerRole;
    User peerUser;

        if (ROLE_RESTAURATEUR.equals(senderRole)) {
            restaurateur = restaurateurMapper.getByUserId(sender.getId());
            if (restaurateur == null) {
                throw new IllegalArgumentException("商家账号未完成资料");
            }
            if (!Objects.equals(order.getRestaurateurId(), restaurateur.getId())) {
                throw new IllegalArgumentException("订单不属于当前商家");
            }
            restaurateurId = restaurateur.getId();
            peerRole = receiverRole;
            peerUser = userMapper.getById(receiverId);
            if (peerUser == null) {
                throw new IllegalArgumentException("接收人不存在");
            }
        } else {
            restaurateurId = order.getRestaurateurId();
            if (restaurateurId == null) {
                throw new IllegalArgumentException("订单缺少商家信息");
            }
            restaurateur = restaurateurMapper.getById(restaurateurId);
            if (restaurateur == null) {
                throw new IllegalArgumentException("商家不存在");
            }
            peerRole = senderRole;
            peerUser = sender;
        }

        ChatSession session = ensureSession(restaurateurId, order, peerRole, peerUser);
        if (session == null) {
            throw new IllegalArgumentException("无法创建会话");
        }

        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setOrderId(orderId);
        message.setContextType(order != null ? "ORDER" : null);
        message.setContextRef(order != null ? order.getId() : null);
        message.setSenderId(sender.getId());
        message.setSenderRole(senderRole);
        message.setReceiverId(receiverId);
        message.setReceiverRole(receiverRole);
        message.setContent(content);
        message.setReadFlag(Boolean.FALSE);
        message.setCreatedAt(LocalDateTime.now());

        chatMessageMapper.insert(message);

        String preview = shorten(content);
        chatSessionMapper.updateLastMessage(session.getId(), preview, message.getCreatedAt());

        ChatMessageDto dto = mapToDto(message);
        dto.setSessionId(session.getSessionKey());

        Map<Integer, OrderInfo> orderCache = new HashMap<>();
        Map<Integer, User> userCache = new HashMap<>();
        Map<Integer, Integer> restaurateurUserIdCache = new HashMap<>();

        Integer senderUnreadKey = ROLE_RESTAURATEUR.equals(senderRole) ? restaurateurId : sender.getId();
        Integer receiverUnreadKey = ROLE_RESTAURATEUR.equals(receiverRole) ? restaurateurId : receiverId;

        ChatSessionSummary senderSummary = buildSummaryForViewer(
            session,
            senderRole,
            senderUnreadKey,
            orderCache,
            userCache,
            restaurateurUserIdCache
        );

        ChatSessionSummary receiverSummary = buildSummaryForViewer(
            session,
            receiverRole,
            receiverUnreadKey,
            orderCache,
            userCache,
            restaurateurUserIdCache
        );

        Integer receiverUserId = ROLE_RESTAURATEUR.equals(receiverRole)
            ? resolveRestaurateurUserId(restaurateurId, restaurateurUserIdCache)
            : receiverId;
        User receiver = resolveUser(receiverUserId, userCache);
        String receiverUsername = receiver == null ? null : receiver.getUsername();

        return ChatMessageDelivery.builder()
            .message(dto)
            .senderSession(senderSummary)
            .receiverSession(receiverSummary)
            .receiverUsername(receiverUsername)
            .senderUsername(sender.getUsername())
            .build();
    }

    private ChatSession ensureSession(Integer restaurateurId, Integer orderId, String peerRole, Integer peerId) {
        if (peerId == null) {
            return null;
        }
        User peer = userMapper.getById(peerId);
        if (peer == null) {
            return null;
        }
        OrderInfo order = orderId == null ? null : orderMapper.getById(orderId);
        return ensureSession(restaurateurId, order, peerRole, peer);
    }

    private ChatSession ensureSession(Integer restaurateurId, OrderInfo order, String peerRole, User peer) {
        if (peer == null) {
            return null;
        }
            String sessionKey = buildSessionKey(null, peerRole, peer.getId());
        ChatSession session = chatSessionMapper.findByKey(sessionKey);
        if (session != null) {
            // 已存在统一会话时，如提供了订单上下文且与当前会话不一致，则更新为最新上下文与标题
            if (order != null && (session.getOrderId() == null || !Objects.equals(session.getOrderId(), order.getId()))) {
                chatSessionMapper.updateTitle(session.getId(), buildSessionTitle(peer, order), "ORDER", order.getId(), order.getId());
                session.setOrderId(order.getId());
                session.setContextType("ORDER");
                session.setContextRef(order.getId());
                session.setTitle(buildSessionTitle(peer, order));
            }
            return session;
        }

        ChatSession created = new ChatSession();
        created.setSessionKey(sessionKey);
        created.setRestaurateurId(restaurateurId);
        created.setPeerUserId(peer.getId());
        created.setPeerRole(peerRole);
        created.setOrderId(order == null ? null : order.getId());
        created.setContextType(order == null ? null : "ORDER");
        created.setContextRef(order == null ? null : order.getId());
        created.setTitle(buildSessionTitle(peer, order));
        LocalDateTime now = LocalDateTime.now();
        created.setLastMessagePreview(null);
        created.setLastMessageTime(null);
        created.setCreatedAt(now);
        created.setUpdatedAt(now);
        chatSessionMapper.insert(created);
        return Optional.ofNullable(created.getId())
            .map(id -> created)
            .orElseGet(() -> chatSessionMapper.findByKey(sessionKey));
    }

    private String buildSessionKey(Integer orderId, String peerRole, Integer peerId) {
        return "FREE:" + peerRole + ":" + peerId;
    }

    private OrderInfo resolveOrder(Integer orderId, Map<Integer, OrderInfo> cache) {
        if (orderId == null) {
            return null;
        }
        return cache.computeIfAbsent(orderId, id -> orderMapper.getById(id));
    }

    private User resolveUser(Integer userId, Map<Integer, User> cache) {
        if (userId == null) {
            return null;
        }
        return cache.computeIfAbsent(userId, id -> userMapper.getById(id));
    }

    private Integer resolveRestaurateurUserId(Integer restaurateurId, Map<Integer, Integer> cache) {
        if (restaurateurId == null) {
            return null;
        }
        return cache.computeIfAbsent(restaurateurId, id -> restaurateurMapper.getUserIdByRestaurateurId(id));
    }

    private ChatSessionSummary buildSummaryForViewer(ChatSession session,
                                                    String viewerRole,
                                                    Integer receiverId,
                                                    Map<Integer, OrderInfo> orderCache,
                                                    Map<Integer, User> userCache,
                                                    Map<Integer, Integer> restaurateurUserIdCache) {
        OrderInfo order = resolveOrder(session.getOrderId(), orderCache);
        String preview = session.getLastMessagePreview();
        LocalDateTime time = session.getLastMessageTime();

        Integer peerId;
        Integer displayUserId;
        String counterpartRole;
        if (ROLE_RESTAURATEUR.equals(viewerRole)) {
            peerId = session.getPeerUserId();
            displayUserId = session.getPeerUserId();
            counterpartRole = session.getPeerRole();
        } else {
            displayUserId = resolveRestaurateurUserId(session.getRestaurateurId(), restaurateurUserIdCache);
            peerId = displayUserId;
            counterpartRole = ROLE_RESTAURATEUR;
        }
        if (peerId == null) {
            return null;
        }

        User counterpart = resolveUser(displayUserId, userCache);
        Integer unread = chatMessageMapper.countUnread(session.getId(), viewerRole, receiverId);

        return ChatSessionSummary.builder()
            .sessionId(session.getSessionKey())
            .orderId(session.getOrderId())
            .peerId(peerId)
            .peerRole(counterpartRole)
            .peerName(counterpart == null ? null : peerDisplayName(counterpart))
            .peerAvatar(counterpart == null ? null : counterpart.getAvatarUrl())
            .orderNo(order == null ? null : order.getOrderNo())
            .orderStatus(order == null ? null : order.getStatus())
            .orderRemark(order == null ? null : order.getRemark())
            .lastMessage(preview)
            .lastMessageTime(time)
            .unreadCount(unread == null ? 0 : unread)
            .build();
    }

    private Map<String, Object> orderSummary(OrderInfo order) {
        Map<String, Object> summary = new HashMap<>();
        summary.put("orderId", order.getId());
        summary.put("orderNo", order.getOrderNo());
        summary.put("status", order.getStatus());
        summary.put("remark", order.getRemark());
        summary.put("createdAt", order.getCreatedAt());
        return summary;
    }

    private ChatMessageDto mapToDto(ChatMessage message) {
        return ChatMessageDto.builder()
            .id(message.getId())
            .orderId(message.getOrderId())
            .senderId(message.getSenderId())
            .senderRole(message.getSenderRole())
            .receiverId(message.getReceiverId())
            .receiverRole(message.getReceiverRole())
            .content(message.getContent())
            .read(Boolean.TRUE.equals(message.getReadFlag()))
            .createdAt(message.getCreatedAt())
            .build();
    }

    private Map<String, Object> safeData(DataRequest request) {
        return request == null ? Collections.emptyMap() : (request.getData() == null ? Collections.emptyMap() : request.getData());
    }

    private String stringValue(Object o) {
        return o == null ? null : Objects.toString(o, null);
    }

    private Integer intValue(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(o.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    private String peerDisplayName(User user) {
        if (user == null) {
            return null;
        }
        return user.getNickname() != null && !user.getNickname().isBlank() ? user.getNickname() : user.getUsername();
    }

    private String buildSessionTitle(User peer, OrderInfo order) {
        StringBuilder builder = new StringBuilder();
        if (peer != null) {
            builder.append(peerDisplayName(peer));
        }
        if (order != null && order.getOrderNo() != null) {
            if (builder.length() > 0) {
                builder.append(" · ");
            }
            builder.append("订单").append(order.getOrderNo());
        }
        return builder.toString();
    }

    private String shorten(String content) {
        if (content == null) {
            return null;
        }
        if (content.length() <= PREVIEW_MAX) {
            return content;
        }
        return content.substring(0, PREVIEW_MAX) + "…";
    }
}
