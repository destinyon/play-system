package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.entity.Deliveryman;
import com.example.entity.Restaurant;
import com.example.entity.Restaurateur;
import com.example.entity.User;
import com.example.entity.UserRole;
import com.example.mapper.DeliverymanMapper;
import com.example.mapper.RestaurantMapper;
import com.example.mapper.RestaurateurMapper;
import com.example.mapper.UserMapper;
import com.example.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.example.util.FileUtil;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String AVATAR_SUBDIR = "avatars";
    private static final Path AVATAR_ROOT = Paths.get("uploads", AVATAR_SUBDIR);

    private final UserMapper userMapper;
    private final RestaurateurMapper restaurateurMapper;
    private final DeliverymanMapper deliverymanMapper;
    private final RestaurantMapper restaurantMapper;

    @Override
    public Result loginService(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data == null) {
            return Result.error("请求数据为空");
        }

        String username = stringValue(data.get("username"));
        String password = stringValue(data.get("password"));
        if (username == null || password == null) {
            return Result.error("用户名或密码不能为空");
        }

        User user = userMapper.getByUsernameAndPassword(username, password);
        if (user == null) {
            return Result.error("用户名或密码错误");
        }

        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            user.setNickname(user.getUsername());
        }
        return Result.success("登录成功", buildUserResponse(user));
    }

    @Override
    @Transactional
    public Result registerService(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data == null) {
            return Result.error("请求数据为空");
        }

        String username = stringValue(data.get("username"));
        String password = stringValue(data.get("password"));
        if (username == null || password == null) {
            return Result.error("用户名或密码不能为空");
        }

        if (userMapper.getByUsername(username) != null) {
            return Result.error("用户名已存在");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(stringValue(data.get("role"), "GUEST").toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Result.error("角色不合法");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        String nickname = stringValue(data.get("nickname"));
        newUser.setNickname((nickname == null || nickname.isEmpty()) ? username : nickname);
        newUser.setRole(role);
        newUser.setAddress(stringValue(data.get("address")));
        newUser.setEmail(stringValue(data.get("email")));
        newUser.setPhone(stringValue(data.get("phone")));
        newUser.setAvatarUrl(extractFilename(stringValue(data.get("avatarUrl"))));

        int inserted = userMapper.insertUser(newUser);
        if (inserted <= 0 || newUser.getId() == null) {
            return Result.error("注册失败");
        }

        if (role == UserRole.RESTAURATEUR) {
            Restaurateur restaurateur = new Restaurateur();
            restaurateur.setUser(newUser);
            restaurateurMapper.insert(restaurateur);

            // Only create a Restaurant record here if the registration payload contains
            // the required restaurant fields. The frontend may create the restaurant
            // via a separate /api/restaurant/createOrUpdate call instead.
            String restName = stringValue(data.get("restaurantName"));
            String restAddress = stringValue(data.get("restaurantAddress"));
            Double restLng = doubleValue(data.get("lng"));
            Double restLat = doubleValue(data.get("lat"));
            if (restName != null && restAddress != null && restLng != null && restLat != null) {
                Restaurant restaurant = new Restaurant();
                restaurant.setRestaurantName(restName);
                restaurant.setRestaurantAddress(restAddress);
                restaurant.setRestaurantImageUrl(stringValue(data.get("restaurantImageUrl")));
                restaurant.setLng(restLng);
                restaurant.setLat(restLat);
                restaurant.setRestaurateur(restaurateur);
                restaurantMapper.insert(restaurant);
            } else {
                // No restaurant payload provided during registration; frontend is expected
                // to call /api/restaurant/createOrUpdate separately when merchant fills details.
            }
        } else if (role == UserRole.DELIVERYMAN) {
            Deliveryman deliveryman = new Deliveryman();
            deliveryman.setUser(newUser);
            deliveryman.setIncome(doubleValue(data.get("income")));
            deliverymanMapper.insert(deliveryman);
        }

        return Result.success("注册成功", buildUserResponse(newUser));
    }

    @Override
    public Result getById(DataRequest dataRequest) {
        Integer id = intValueFromData(dataRequest, "id");
        if (id == null) {
            return Result.error("缺少id");
        }
        User user = userMapper.getById(id);
        if (user == null) {
            return Result.error("用户未找到");
        }
        return Result.success(buildUserResponse(user));
    }

    @Override
    public Result getByUsername(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data == null) {
            return Result.error("请求数据为空");
        }
        String username = stringValue(data.get("username"));
        if (username == null) {
            return Result.error("缺少username");
        }
        User user = userMapper.getByUsername(username);
        if (user == null) {
            return Result.error("用户未找到");
        }
        return Result.success(buildUserResponse(user));
    }

    @Override
    public Result getAllUsers() {
        List<User> users = userMapper.getAllUsers();
        List<Map<String, Object>> payload = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
                payload.add(buildUserResponse(user));
            }
        }
        return Result.success(payload);
    }

    @Transactional
    public Result deleteUserById(DataRequest dataRequest) {
        Integer userId = intValueFromData(dataRequest, "id");
        if (userId == null) {
            return Result.error("缺少id");
        }

        Restaurateur restaurateur = restaurateurMapper.getByUserId(userId);
        if (restaurateur != null) {
            restaurantMapper.deleteByRestaurateurId(restaurateur.getId());
            restaurateurMapper.deleteByUserId(userId);
        }
        deliverymanMapper.deleteByUserId(userId);
        userMapper.deleteById(userId);
        return Result.success("删除成功", null);
    }

    @Transactional
    public Result deleteRestaurateurByUserId(DataRequest dataRequest) {
        Integer userId = intValueFromData(dataRequest, "userId");
        if (userId == null) {
            return Result.error("缺少userId");
        }
        Restaurateur restaurateur = restaurateurMapper.getByUserId(userId);
        if (restaurateur != null) {
            restaurantMapper.deleteByRestaurateurId(restaurateur.getId());
            restaurateurMapper.deleteByUserId(userId);
        }
        return Result.success("删除成功", null);
    }

    @Override
    public Result updateUser(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data == null) {
            return Result.error("请求数据为空");
        }
        String username = stringValue(data.get("username"));
        if (username == null) {
            return Result.error("缺少username");
        }
        User user = userMapper.getByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        // keep original avatar to compare later
        String originalAvatar = user.getAvatarUrl();

        if (data.containsKey("nickname")) user.setNickname(stringValue(data.get("nickname")));
        if (data.containsKey("email")) user.setEmail(stringValue(data.get("email")));
        if (data.containsKey("phone")) user.setPhone(stringValue(data.get("phone")));
        if (data.containsKey("address")) user.setAddress(stringValue(data.get("address")));
        String newAvatar = null;
        if (data.containsKey("avatarUrl")) {
            newAvatar = extractFilename(stringValue(data.get("avatarUrl")));
            user.setAvatarUrl(newAvatar);
        }

        int rows = userMapper.update(user);
        if (rows <= 0) {
            return Result.error("更新失败");
        }
        // If avatar changed and the previous avatar was a local file, delete it to avoid orphaned files
        if (newAvatar != null && originalAvatar != null && !originalAvatar.equals(newAvatar)) {
            String prevFilename = extractFilename(originalAvatar);
            if (prevFilename != null) {
                try {
                    FileUtil.deleteFile(prevFilename, AVATAR_SUBDIR);
                } catch (Exception ignored) {}
            }
        }
        return Result.success("更新成功", buildUserResponse(user));
    }

    @Override
    public Result changePassword(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data == null) {
            return Result.error("请求数据为空");
        }
        String username = stringValue(data.get("username"));
        String oldPassword = stringValue(data.get("oldPassword"));
        String newPassword = stringValue(data.get("newPassword"));
        if (username == null || oldPassword == null || newPassword == null) {
            return Result.error("参数不完整");
        }

        User user = userMapper.getByUsernameAndPassword(username, oldPassword);
        if (user == null) {
            return Result.error("当前密码错误");
        }

        user.setPassword(newPassword);
        int rows = userMapper.update(user);
        if (rows <= 0) {
            return Result.error("密码修改失败");
        }
        return Result.success("密码修改成功", true);
    }

    @Override
    public Result getUserStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", getTotalUserCount());
        stats.put("guestCount", getUserCountByRole("GUEST"));
        stats.put("restaurateurCount", getUserCountByRole("RESTAURATEUR"));
        stats.put("deliverymanCount", getUserCountByRole("DELIVERYMAN"));
        stats.put("adminCount", getUserCountByRole("ADMIN"));
        return Result.success(stats);
    }

    @Override
    public int getTotalUserCount() {
        List<User> users = userMapper.getAllUsers();
        return users == null ? 0 : users.size();
    }

    @Override
    public int getUserCountByRole(String role) {
        if (role == null) {
            return 0;
        }
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            return userMapper.countByRole(userRole);
        } catch (IllegalArgumentException ex) {
            return 0;
        }
    }

    @Override
    public Result uploadAvatar(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件为空");
        }
        try {
            String filename = FileUtil.saveFile(file, AVATAR_SUBDIR);
            Map<String, String> payload = new HashMap<>();
            payload.put("filename", filename);
            payload.put("url", "/api/user/avatar/" + filename);
            return Result.success("头像上传成功", payload);
        } catch (IOException ex) {
            return Result.error("上传失败: " + ex.getMessage());
        }
    }

    @Override
    public Result getAvatar(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return Result.error("文件名不能为空");
        }
        try {
            Path directory = AVATAR_ROOT.toAbsolutePath().normalize();
            Path filePath = directory.resolve(filename.trim()).normalize();
            if (!filePath.startsWith(directory) || !Files.exists(filePath)) {
                return Result.error("头像不存在");
            }

            byte[] bytes = Files.readAllBytes(filePath);
            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put("bytes", bytes);
            payload.put("contentType", contentType);
            payload.put("filename", filename.trim());
            payload.put("base64", Base64.getEncoder().encodeToString(bytes));
            return Result.success("头像获取成功", payload);
        } catch (IOException ex) {
            return Result.error("读取头像失败: " + ex.getMessage());
        }
    }

    private Map<String, Object> buildUserResponse(User user) {
        Map<String, Object> map = new HashMap<>();
        if (user == null) {
            return map;
        }
        map.put("id", user.getId());
        map.put("username", user.getUsername());
        map.put("nickname", user.getNickname());
        map.put("role", user.getRole() == null ? null : user.getRole().name());
        map.put("address", user.getAddress());
        map.put("email", user.getEmail());
        map.put("phone", user.getPhone());
        map.put("avatarUrl", formatAvatarUrl(user.getAvatarUrl()));
        return map;
    }

    private String formatAvatarUrl(String storedValue) {
        if (storedValue == null || storedValue.trim().isEmpty()) {
            return null;
        }
        String trimmed = storedValue.trim();
        if (trimmed.startsWith("http") || trimmed.startsWith("/api/user/avatar/")) {
            return trimmed;
        }
        if (trimmed.startsWith("/uploads/avatars/")) {
            trimmed = trimmed.substring("/uploads/avatars/".length());
        }
        return "/api/user/avatar/" + trimmed;
    }

    private String extractFilename(String rawAvatar) {
        if (rawAvatar == null || rawAvatar.trim().isEmpty()) {
            return null;
        }
        String value = rawAvatar.trim();
        if (value.startsWith("/api/user/avatar/")) {
            return value.substring("/api/user/avatar/".length());
        }
        if (value.startsWith("/uploads/avatars/")) {
            return value.substring("/uploads/avatars/".length());
        }
        return value;
    }

    private Map<String, Object> safeData(DataRequest request) {
        return request == null ? null : request.getData();
    }

    private Integer intValueFromData(DataRequest request, String key) {
        Map<String, Object> data = safeData(request);
        if (data == null) {
            return null;
        }
        Object value = data.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String str = value.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private String stringValue(Object value, String defaultValue) {
        String str = stringValue(value);
        return str == null ? defaultValue : str;
    }

    private Double doubleValue(Object value) {
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble(((String) value).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}

