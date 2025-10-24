package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;

public interface RestaurateurStatsService {
    Result getStats(DataRequest request);
    Result getMetrics(DataRequest request);
    Result getTopDishes(DataRequest request);
}
