package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.dto.OrderDetailDto;
import com.example.dto.OrderListItemDto;
import com.example.dto.PageResult;
import com.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/pending")
    public Result<PageResult<OrderListItemDto>> getPending(@RequestBody DataRequest request) {
        return orderService.getPendingOrders(request);
    }

    @PostMapping("/list")
    public Result<PageResult<OrderListItemDto>> getList(@RequestBody DataRequest request) {
        return orderService.getOrderList(request);
    }

    @PostMapping("/detail")
    public Result<OrderDetailDto> getDetail(@RequestBody DataRequest request) {
        return orderService.getOrderDetail(request);
    }

    @PostMapping("/accept")
    public Result<Void> accept(@RequestBody DataRequest request) {
        return orderService.acceptOrder(request);
    }

    @PostMapping("/start-cooking")
    public Result<Void> startCooking(@RequestBody DataRequest request) {
        return orderService.startCooking(request);
    }

    @PostMapping("/ready")
    public Result<Void> markReady(@RequestBody DataRequest request) {
        return orderService.markReady(request);
    }

    @PostMapping("/complete")
    public Result<Void> complete(@RequestBody DataRequest request) {
        return orderService.completeOrder(request);
    }
}
