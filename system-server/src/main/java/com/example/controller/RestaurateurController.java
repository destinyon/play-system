package com.example.controller;

import com.example.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/restaurateur")
public class RestaurateurController {

    @GetMapping("/stats")
    public Result getStats() {
        // Mock statistics for now - in production, calculate from orders
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncome", 18500.0);
        stats.put("pendingOrders", 5);
        stats.put("todayOrders", 23);
        stats.put("todayOrdersGrowth", 15);
        stats.put("dishCount", 28);
        
        return Result.success(stats);
    }
}
