package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/pending")
    public Result getPending(@RequestBody DataRequest request) {
        return orderService.getPendingOrders(request);
    }

    @PostMapping("/list")
    public Result getList(@RequestBody DataRequest request) {
        return orderService.getOrderList(request);
    }

    @PostMapping("/detail")
    public Result getDetail(@RequestBody DataRequest request) {
        return orderService.getOrderDetail(request);
    }

    @PostMapping("/accept")
    public Result accept(@RequestBody DataRequest request) {
        return orderService.acceptOrder(request);
    }

    @PostMapping("/start-cooking")
    public Result startCooking(@RequestBody DataRequest request) {
        return orderService.startCooking(request);
    }

    @PostMapping("/ready")
    public Result markReady(@RequestBody DataRequest request) {
        return orderService.markReady(request);
    }

    @PostMapping("/complete")
    public Result complete(@RequestBody DataRequest request) {
        return orderService.completeOrder(request);
    }
}
