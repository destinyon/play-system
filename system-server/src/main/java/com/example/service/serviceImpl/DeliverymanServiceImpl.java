package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.service.DeliverymanService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DeliverymanServiceImpl implements DeliverymanService {

    @Override
    public Result getStats(DataRequest request) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalIncome", 6500.0);
        stats.put("pendingDeliveries", 2);
        stats.put("todayDeliveries", 15);
        stats.put("todayGrowth", 20);
        stats.put("completionRate", 98);
        return Result.success(stats);
    }
}
