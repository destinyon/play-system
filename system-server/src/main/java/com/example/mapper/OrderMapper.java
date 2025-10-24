package com.example.mapper;

import com.example.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface OrderMapper {
    List<OrderInfo> listByRestaurateur(@Param("restaurateurId") Integer restaurateurId);

    List<OrderInfo> listByGuest(@Param("userId") Integer userId);

    List<OrderInfo> listByDeliveryman(@Param("deliverymanId") Integer deliverymanId);

    OrderInfo getById(@Param("id") Integer id);

    List<OrderInfo> pageByStatus(@Param("restaurateurId") Integer restaurateurId,
                                 @Param("status") String status,
                                 @Param("keyword") String keyword,
                                 @Param("offset") Integer offset,
                                 @Param("limit") Integer limit);

    Long countByStatus(@Param("restaurateurId") Integer restaurateurId,
                       @Param("status") String status,
                       @Param("keyword") String keyword);

    List<com.example.dto.OrderStatusCount> countGroupByStatus(@Param("restaurateurId") Integer restaurateurId);

    int updateStatus(@Param("orderId") Integer orderId,
                     @Param("fromStatus") String fromStatus,
                     @Param("toStatus") String toStatus);
}
