package com.example.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.example.entity.User;
import com.example.entity.UserRole;

@Mapper
public interface UserMapper {
    User getById(Integer id);
    User getByUsername(String username);
    List<User> getAllUsers();
    User getByUsernameAndPassword(@Param("username") String username, @Param("password") String password);
    int insertUser(User user);
    int update(User user);
    int deleteById(Integer id);
    int countByRole(UserRole role);
}
