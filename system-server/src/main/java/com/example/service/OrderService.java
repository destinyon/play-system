package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;

public interface OrderService {
    
    Result getPendingOrders(DataRequest request);
    
    Result getOrderList(DataRequest request);
    
    Result getOrderDetail(DataRequest request);
    
    Result acceptOrder(DataRequest request);
    
    Result startCooking(DataRequest request);
    
    Result markReady(DataRequest request);
    
    Result completeOrder(DataRequest request);
}
