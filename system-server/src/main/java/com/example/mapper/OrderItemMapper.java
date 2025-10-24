package com.example.mapper;

import com.example.entity.OrderItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderItemMapper {

    int insert(OrderItem item);

    List<OrderItem> listByOrderId(@Param("orderId") Integer orderId);

    int deleteByOrderId(@Param("orderId") Integer orderId);
}
