package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;

import java.util.Map;

public interface DeliverymanService {
    Result<Map<String, Object>> getStats(DataRequest request);
}
