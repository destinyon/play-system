package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.OrderDetailDto;
import com.example.dto.OrderItemDto;
import com.example.dto.OrderListItemDto;
import com.example.dto.PageResult;
import com.example.entity.OrderInfo;
import com.example.entity.OrderItem;
import com.example.entity.User;
import com.example.mapper.OrderItemMapper;
import com.example.mapper.OrderMapper;
import com.example.mapper.UserMapper;
import com.example.security.RequestDataHelper;
import com.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

    private final OrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final UserMapper userMapper;

    @Override
    public Result<PageResult<OrderListItemDto>> getPendingOrders(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        if (restaurateurId == null) {
            return Result.error("restaurateurId is required");
        }

        String keyword = trimToNull(data.get("keyword"));
        int page = request != null && request.getPage() != null ? request.getPage() : 1;
        int size = request != null && request.getSize() != null ? request.getSize() : 10;

        PageResult<OrderListItemDto> result = pageByStatus(restaurateurId, "PENDING", keyword, page, size);
        return Result.success(result);
    }

    @Override
    public Result<PageResult<OrderListItemDto>> getOrderList(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        if (restaurateurId == null) {
            return Result.error("restaurateurId is required");
        }

        String status = trimToNull(data.get("status"));
        String keyword = trimToNull(data.get("keyword"));
        int page = request != null && request.getPage() != null ? request.getPage() : 1;
        int size = request != null && request.getSize() != null ? request.getSize() : 10;

        PageResult<OrderListItemDto> result = pageByStatus(restaurateurId, status, keyword, page, size);
        return Result.success(result);
    }

    @Override
    public Result<OrderDetailDto> getOrderDetail(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = getIntValue(data, "orderId");
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        if (orderId == null) {
            return Result.error("orderId is required");
        }
        try {
            OrderDetailDto detail = getDetail(orderId, restaurateurId);
            return Result.success(detail);
        } catch (IllegalArgumentException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> acceptOrder(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = getIntValue(data, "orderId");
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        if (orderId == null || restaurateurId == null) {
            return Result.error("orderId and restaurateurId are required");
        }
        try {
            acceptOrderInternal(orderId, restaurateurId);
            return Result.success("accepted");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> startCooking(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = getIntValue(data, "orderId");
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        if (orderId == null || restaurateurId == null) {
            return Result.error("orderId and restaurateurId are required");
        }
        try {
            startCookingInternal(orderId, restaurateurId);
            return Result.success("started");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> markReady(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = getIntValue(data, "orderId");
        Integer restaurateurId = getIntValue(data, "restaurateurId");
        if (orderId == null || restaurateurId == null) {
            return Result.error("orderId and restaurateurId are required");
        }
        try {
            markReadyInternal(orderId, restaurateurId);
            return Result.success("ready");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Result.error(ex.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Void> completeOrder(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer orderId = getIntValue(data, "orderId");
        if (orderId == null) {
            return Result.error("orderId is required");
        }
        try {
            completeOrderInternal(orderId);
            return Result.success("completed");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            return Result.error(ex.getMessage());
        }
    }

    private PageResult<OrderListItemDto> pageByStatus(Integer restaurateurId, String status, String keyword, int page, int size) {
        if (restaurateurId == null) {
            return new PageResult<>(0, Collections.emptyList());
        }
        int pageNum = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNum - 1) * pageSize;

        String normalizedKeyword = trimToNull(keyword);
        String normalizedStatus = trimToNull(status);

        List<OrderInfo> orders = orderMapper.pageByStatus(restaurateurId, normalizedStatus, normalizedKeyword, offset, pageSize);
        Long total = orderMapper.countByStatus(restaurateurId, normalizedStatus, normalizedKeyword);
        List<OrderListItemDto> records = orders.stream().map(this::toListItem).collect(Collectors.toList());
        return new PageResult<>(total, records);
    }

    private OrderDetailDto getDetail(Integer orderId, Integer restaurateurId) {
        OrderInfo order = orderMapper.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }

        if (restaurateurId != null && !restaurateurId.equals(order.getRestaurateurId())) {
            throw new IllegalArgumentException("forbidden");
        }

        List<OrderItem> items = orderItemMapper.listByOrderId(orderId);
        OrderDetailDto dto = new OrderDetailDto();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setStatus(order.getStatus());
        dto.setRemark(order.getRemark());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setUpdatedAt(order.getUpdatedAt());

        if (order.getUserId() != null) {
            User user = userMapper.getById(order.getUserId());
            if (user != null) {
                dto.setCustomerName(user.getNickname() != null ? user.getNickname() : user.getUsername());
                dto.setCustomerPhone(user.getPhone());
            }
        }

        List<OrderItemDto> itemDtos = items.stream()
            .map(item -> {
                OrderItemDto itemDto = new OrderItemDto();
                itemDto.setId(item.getId());
                itemDto.setDishId(item.getDishId());
                itemDto.setDishName(item.getDishName());
                itemDto.setDishImage(item.getDishImage());
                itemDto.setUnitPrice(item.getUnitPrice());
                itemDto.setQuantity(item.getQuantity());
                return itemDto;
            })
            .collect(Collectors.toList());
        dto.setItems(itemDtos);
        return dto;
    }

    private void acceptOrderInternal(Integer orderId, Integer restaurateurId) {
        validateOrderOwnership(orderId, restaurateurId);
        int updated = orderMapper.updateStatus(orderId, "PENDING", "PROCESSING");
        if (updated == 0) {
            throw new IllegalStateException("order is not pending");
        }
    }

    private void startCookingInternal(Integer orderId, Integer restaurateurId) {
        validateOrderOwnership(orderId, restaurateurId);
        OrderInfo order = orderMapper.getById(orderId);
        if (order == null || !"PROCESSING".equals(order.getStatus())) {
            throw new IllegalStateException("order status must be PROCESSING");
        }
    }

    private void markReadyInternal(Integer orderId, Integer restaurateurId) {
        validateOrderOwnership(orderId, restaurateurId);
        int updated = orderMapper.updateStatus(orderId, "PROCESSING", "READY");
        if (updated == 0) {
            throw new IllegalStateException("order is not PROCESSING");
        }
    }

    private void completeOrderInternal(Integer orderId) {
        int updated = orderMapper.updateStatus(orderId, "READY", "COMPLETED");
        if (updated == 0) {
            throw new IllegalStateException("order is not READY");
        }
    }

    private void validateOrderOwnership(Integer orderId, Integer restaurateurId) {
        if (orderId == null || restaurateurId == null) {
            throw new IllegalArgumentException("orderId and restaurateurId are required");
        }
        OrderInfo order = orderMapper.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("order not found");
        }
        if (!restaurateurId.equals(order.getRestaurateurId())) {
            throw new IllegalArgumentException("forbidden");
        }
    }

    private OrderListItemDto toListItem(OrderInfo order) {
        if (order == null) {
            return null;
        }
        OrderListItemDto dto = new OrderListItemDto();
        dto.setId(order.getId());
        dto.setOrderNo(order.getOrderNo());
        dto.setStatus(order.getStatus());
        dto.setDeliveryAddress(order.getDeliveryAddress());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setCreatedAt(order.getCreatedAt());

        if (order.getUserId() != null) {
            User user = userMapper.getById(order.getUserId());
            if (user != null) {
                dto.setCustomerName(user.getNickname() != null ? user.getNickname() : user.getUsername());
            }
        }
        return dto;
    }

    private Map<String, Object> safeData(DataRequest request) {
        return RequestDataHelper.resolve(request);
    }

    private Integer getIntValue(Map<String, Object> data, String key) {
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String trimToNull(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isEmpty() ? null : text;
    }
}
