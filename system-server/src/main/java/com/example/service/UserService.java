package com.example.service;

import com.example.common.DataRequest;
import com.example.common.Result;
import org.springframework.web.multipart.MultipartFile;
public interface UserService {
    Result loginService(DataRequest dataRequest);
    Result registerService(DataRequest dataRequest);
    Result getById(DataRequest dataRequest);
    Result getByUsername(DataRequest dataRequest);
    Result getAllUsers();
    Result updateUser(DataRequest dataRequest);
    Result changePassword(DataRequest dataRequest);
    Result getUserStats();
    Result uploadAvatar(MultipartFile file);
    Result getAvatar(String filename);
    int getTotalUserCount();
    int getUserCountByRole(String role);
}
