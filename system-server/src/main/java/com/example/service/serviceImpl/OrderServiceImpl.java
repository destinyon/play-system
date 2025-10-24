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
    public Result getPendingOrders(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return Result.error("请求数据不能为空");
            }
            
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            if (restaurateurId == null) {
                return Result.error("商家ID不能为空");
            }
            
            String keyword = (String) data.get("keyword");
            int page = request.getPage() != null ? request.getPage() : 1;
            int size = request.getSize() != null ? request.getSize() : 10;
            
            PageResult<OrderListItemDto> result = pageByStatus(restaurateurId, "PENDING", keyword, page, size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询待接单订单失败: " + e.getMessage());
        }
    }

    @Override
    public Result getOrderList(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return Result.error("请求数据不能为空");
            }
            
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            if (restaurateurId == null) {
                return Result.error("商家ID不能为空");
            }
            
            String status = (String) data.get("status");
            String keyword = (String) data.get("keyword");
            int page = request.getPage() != null ? request.getPage() : 1;
            int size = request.getSize() != null ? request.getSize() : 10;
            
            PageResult<OrderListItemDto> result = pageByStatus(restaurateurId, status, keyword, page, size);
            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询订单列表失败: " + e.getMessage());
        }
    }

    @Override
    public Result getOrderDetail(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return Result.error("请求数据不能为空");
            }
            
            Integer orderId = getIntValue(data, "orderId");
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            
            if (orderId == null) {
                return Result.error("订单ID不能为空");
            }
            
            OrderDetailDto detail = getDetail(orderId, restaurateurId);
            return Result.success(detail);
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("查询订单详情失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result acceptOrder(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return Result.error("请求数据不能为空");
            }
            
            Integer orderId = getIntValue(data, "orderId");
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            
            if (orderId == null || restaurateurId == null) {
                return Result.error("订单ID和商家ID不能为空");
            }
            
            acceptOrderInternal(orderId, restaurateurId);
            return Result.success("接单成功");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("接单失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result startCooking(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return Result.error("请求数据不能为空");
            }
            
            Integer orderId = getIntValue(data, "orderId");
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            
            if (orderId == null || restaurateurId == null) {
                return Result.error("订单ID和商家ID不能为空");
            }
            
            startCookingInternal(orderId, restaurateurId);
            return Result.success("开始制作");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result markReady(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return Result.error("请求数据不能为空");
            }
            
            Integer orderId = getIntValue(data, "orderId");
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            
            if (orderId == null || restaurateurId == null) {
                return Result.error("订单ID和商家ID不能为空");
            }
            
            markReadyInternal(orderId, restaurateurId);
            return Result.success("已标记为出餐");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Result completeOrder(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            if (data == null) {
                return Result.error("请求数据不能为空");
            }
            
            Integer orderId = getIntValue(data, "orderId");
            if (orderId == null) {
                return Result.error("订单ID不能为空");
            }
            
            completeOrderInternal(orderId);
            return Result.success("订单已完成");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return Result.error(e.getMessage());
        } catch (Exception e) {
            return Result.error("操作失败: " + e.getMessage());
        }
    }

    // 内部辅助方法
    private PageResult<OrderListItemDto> pageByStatus(Integer restaurateurId, String status, String keyword, int page, int size) {
        if (restaurateurId == null) {
            return new PageResult<>(0, Collections.emptyList());
        }
        int pageNum = Math.max(page, 1);
        int pageSize = Math.max(size, 1);
        int offset = (pageNum - 1) * pageSize;
        
        String normalizedKeyword = (keyword != null && !keyword.trim().isEmpty()) ? keyword.trim() : null;
        String normalizedStatus = (status != null && !status.trim().isEmpty()) ? status.trim() : null;
        
        List<OrderInfo> orders = orderMapper.pageByStatus(restaurateurId, normalizedStatus, normalizedKeyword, offset, pageSize);
        Long total = orderMapper.countByStatus(restaurateurId, normalizedStatus, normalizedKeyword);
        
        List<OrderListItemDto> records = orders.stream()
                .map(this::toListItem)
                .collect(Collectors.toList());
        
        return new PageResult<>(total, records);
    }

    private OrderDetailDto getDetail(Integer orderId, Integer restaurateurId) {
        if (orderId == null) {
            throw new IllegalArgumentException("订单ID不能为空");
        }
        
        OrderInfo order = orderMapper.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        
        if (restaurateurId != null && !restaurateurId.equals(order.getRestaurateurId())) {
            throw new IllegalArgumentException("无权访问此订单");
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
            throw new IllegalStateException("订单状态不是待接单，无法接单");
        }
    }

    private void startCookingInternal(Integer orderId, Integer restaurateurId) {
        validateOrderOwnership(orderId, restaurateurId);
        OrderInfo order = orderMapper.getById(orderId);
        if (order == null || !"PROCESSING".equals(order.getStatus())) {
            throw new IllegalStateException("订单状态不正确");
        }
    }

    private void markReadyInternal(Integer orderId, Integer restaurateurId) {
        validateOrderOwnership(orderId, restaurateurId);
        int updated = orderMapper.updateStatus(orderId, "PROCESSING", "READY");
        if (updated == 0) {
            throw new IllegalStateException("订单状态不是处理中，无法标记为已出餐");
        }
    }

    private void completeOrderInternal(Integer orderId) {
        int updated = orderMapper.updateStatus(orderId, "READY", "COMPLETED");
        if (updated == 0) {
            throw new IllegalStateException("订单状态不是已出餐，无法完成");
        }
    }

    private void validateOrderOwnership(Integer orderId, Integer restaurateurId) {
        if (orderId == null || restaurateurId == null) {
            throw new IllegalArgumentException("订单ID和商家ID不能为空");
        }
        OrderInfo order = orderMapper.getById(orderId);
        if (order == null) {
            throw new IllegalArgumentException("订单不存在");
        }
        if (!restaurateurId.equals(order.getRestaurateurId())) {
            throw new IllegalArgumentException("无权操作此订单");
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

    private Integer getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
