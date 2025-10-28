package com.example.security;

import com.example.common.DataRequest;

import java.util.HashMap;
import java.util.Map;

public final class RequestDataHelper {

    private RequestDataHelper() {
    }

    public static Map<String, Object> resolve(DataRequest request) {
        Map<String, Object> data;
        if (request == null || request.getData() == null) {
            data = new HashMap<>();
        } else {
            data = new HashMap<>(request.getData());
        }
        SecurityContext.enrich(data);
        if (request != null) {
            request.setData(data);
        }
        return data;
    }
}
