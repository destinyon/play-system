package com.example.controller;

import com.example.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/deliveryman")
public class DeliverymanController {

    @GetMapping("/stats")
    public Result getStats() {
        // Mock statistics for now - in production, calculate from deliveries
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncome", 6500.0);
        stats.put("pendingDeliveries", 2);
        stats.put("todayDeliveries", 15);
        stats.put("todayGrowth", 20);
        stats.put("completionRate", 98);
        
        return Result.success(stats);
    }
}
