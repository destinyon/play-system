package com.example.controller;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.service.RestaurateurStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/restaurateur")
@RequiredArgsConstructor
public class RestaurateurController {

    private final RestaurateurStatsService restaurateurStatsService;

    @PostMapping("/stats")
    public Result getStats(@RequestBody DataRequest request) {
        return restaurateurStatsService.getStats(request);
    }

    @PostMapping("/metrics")
    public Result getMetrics(@RequestBody DataRequest request) {
        return restaurateurStatsService.getMetrics(request);
    }

    @PostMapping("/menu/tops")
    public Result getTopDishes(@RequestBody DataRequest request) {
        return restaurateurStatsService.getTopDishes(request);
    }
}
