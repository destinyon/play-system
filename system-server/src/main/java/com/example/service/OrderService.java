package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.OrderDetailDto;
import com.example.dto.OrderListItemDto;
import com.example.dto.PageResult;

public interface OrderService {

    Result<PageResult<OrderListItemDto>> getPendingOrders(DataRequest request);

    Result<PageResult<OrderListItemDto>> getOrderList(DataRequest request);

    Result<OrderDetailDto> getOrderDetail(DataRequest request);

    Result<Void> acceptOrder(DataRequest request);

    Result<Void> startCooking(DataRequest request);

    Result<Void> markReady(DataRequest request);

    Result<Void> completeOrder(DataRequest request);
}
