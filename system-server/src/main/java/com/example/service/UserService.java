package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.entity.User;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public interface UserService {
    Result<Map<String, Object>> loginService(DataRequest dataRequest);

    Result<Map<String, Object>> registerService(DataRequest dataRequest);

    Result<Map<String, Object>> getById(DataRequest dataRequest);

    Result<Map<String, Object>> getByUsername(DataRequest dataRequest);

    Result<java.util.List<Map<String, Object>>> getAllUsers(DataRequest dataRequest);

    Result<Map<String, Object>> updateUser(DataRequest dataRequest);

    Result<Void> changePassword(DataRequest dataRequest);

    Result<Map<String, Object>> getUserStats(DataRequest dataRequest);

    Result<Map<String, Object>> uploadAvatar(DataRequest dataRequest, MultipartFile file);

    Result<Map<String, Object>> getAvatar(String filename);

    int getTotalUserCount();

    int getUserCountByRole(String role);
}
