package com.example.common;

import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
public class DataRequest {
    private Map<String, Object> data;

    // 常见分页参数
    private Integer page;
    private Integer size;

    public boolean hasPagination() {
        return page != null && size != null;
    }
}
