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
import com.example.security.RequestDataHelper;
import com.example.security.JwtUtil;
import com.example.service.UserService;
import com.example.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String AVATAR_SUBDIR = "avatars";
    private static final Path AVATAR_ROOT = Paths.get("uploads", AVATAR_SUBDIR);

    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[A-Za-z0-9_]{3,32}$");
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("^.{6,64}$");
    private static final Pattern PHONE_PATTERN = Pattern.compile("^1[3-9]\\d{9}$");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");

    private final UserMapper userMapper;
    private final RestaurateurMapper restaurateurMapper;
    private final DeliverymanMapper deliverymanMapper;
    private final RestaurantMapper restaurantMapper;
    private final JwtUtil jwtUtil;

    @Override
    public Result<Map<String, Object>> loginService(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data.isEmpty()) {
            return Result.error("request payload is empty");
        }
        String username = stringValue(data.get("username"));
        String password = stringValue(data.get("password"));
        List<String> errors = new ArrayList<>();
        if (!isValidUsername(username)) {
            errors.add("username must be 3-32 characters (letters, numbers, underscore)");
        }
        if (!isValidPassword(password)) {
            errors.add("password must be at least 6 characters");
        }
        if (!errors.isEmpty()) {
            return Result.error(String.join("; ", errors));
        }

        User user = userMapper.getByUsernameAndPassword(username, password);
        if (user == null) {
            return Result.error("invalid username or password");
        }
        ensureNicknameFallback(user);
        Map<String, Object> payload = buildUserResponse(user);
        JwtUtil.TokenDetails token = jwtUtil.generateToken(user.getUsername(), user.getRole() != null ? user.getRole().name() : null, user.getId() == null ? null : user.getId().longValue(), null);
        attachToken(payload, token);
        return Result.success("login success", payload);
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> registerService(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data.isEmpty()) {
            return Result.error("request payload is empty");
        }

        String username = stringValue(data.get("username"));
        String password = stringValue(data.get("password"));
        String email = stringValue(data.get("email"));
        String phone = stringValue(data.get("phone"));

        List<String> errors = new ArrayList<>();
        if (!isValidUsername(username)) {
            errors.add("username must be 3-32 characters (letters, numbers, underscore)");
        }
        if (!isValidPassword(password)) {
            errors.add("password must be at least 6 characters");
        }
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            errors.add("invalid email format");
        }
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            errors.add("invalid phone format");
        }
        if (!errors.isEmpty()) {
            return Result.error(String.join("; ", errors));
        }

        if (userMapper.getByUsername(username) != null) {
            return Result.error("username already exists");
        }

        UserRole role;
        try {
            role = UserRole.valueOf(stringValue(data.get("role"), "GUEST").toUpperCase());
        } catch (IllegalArgumentException ex) {
            return Result.error("unsupported role");
        }

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        String nickname = stringValue(data.get("nickname"));
        newUser.setNickname(nickname == null || nickname.isEmpty() ? username : nickname);
        newUser.setRole(role);
        newUser.setAddress(stringValue(data.get("address")));
        newUser.setEmail(email);
        newUser.setPhone(phone);
        newUser.setAvatarUrl(extractFilename(stringValue(data.get("avatarUrl"))));

        int inserted = userMapper.insertUser(newUser);
        if (inserted <= 0 || newUser.getId() == null) {
            return Result.error("registration failed");
        }

        if (role == UserRole.RESTAURATEUR) {
            handleRestaurateurRegistration(data, newUser);
        } else if (role == UserRole.DELIVERYMAN) {
            Deliveryman deliveryman = new Deliveryman();
            deliveryman.setUser(newUser);
            deliverymanMapper.insert(deliveryman);
        }

        Map<String, Object> payload = buildUserResponse(newUser);
        JwtUtil.TokenDetails token = jwtUtil.generateToken(newUser.getUsername(),
                newUser.getRole() != null ? newUser.getRole().name() : null,
                newUser.getId() == null ? null : newUser.getId().longValue(),
                null);
        attachToken(payload, token);
        return Result.success("registration success", payload);
    }

    @Override
    public Result<Map<String, Object>> getById(DataRequest dataRequest) {
        Integer id = intValueFromData(dataRequest, "id");
        if (id == null) {
            return Result.error("id is required");
        }
        User user = userMapper.getById(id);
        if (user == null) {
            return Result.error("user not found");
        }
        ensureNicknameFallback(user);
        return Result.success(buildUserResponse(user));
    }

    @Override
    public Result<Map<String, Object>> getByUsername(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        String username = stringValue(data.get("username"));
        if (username == null) {
            return Result.error("username is required");
        }
        User user = userMapper.getByUsername(username);
        if (user == null) {
            return Result.error("user not found");
        }
        ensureNicknameFallback(user);
        return Result.success(buildUserResponse(user));
    }

    @Override
    public Result<List<Map<String, Object>>> getAllUsers(DataRequest dataRequest) {
        List<User> users = userMapper.getAllUsers();
        List<Map<String, Object>> payload = new ArrayList<>();
        if (users != null) {
            for (User user : users) {
                ensureNicknameFallback(user);
                payload.add(buildUserResponse(user));
            }
        }
        return Result.success(payload);
    }

    @Override
    public Result<Map<String, Object>> updateUser(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data.isEmpty()) {
            return Result.error("request payload is empty");
        }
        String username = stringValue(data.get("username"));
        if (username == null) {
            return Result.error("username is required");
        }
        User user = userMapper.getByUsername(username);
        if (user == null) {
            return Result.error("user not found");
        }

        String email = stringValue(data.get("email"));
        String phone = stringValue(data.get("phone"));
        if (email != null && !EMAIL_PATTERN.matcher(email).matches()) {
            return Result.error("invalid email format");
        }
        if (phone != null && !PHONE_PATTERN.matcher(phone).matches()) {
            return Result.error("invalid phone format");
        }

        String originalAvatar = user.getAvatarUrl();
        if (data.containsKey("nickname")) {
            user.setNickname(stringValue(data.get("nickname")));
        }
        if (data.containsKey("email")) {
            user.setEmail(email);
        }
        if (data.containsKey("phone")) {
            user.setPhone(phone);
        }
        if (data.containsKey("address")) {
            user.setAddress(stringValue(data.get("address")));
        }

        String newAvatar = null;
        if (data.containsKey("avatarUrl")) {
            newAvatar = extractFilename(stringValue(data.get("avatarUrl")));
            user.setAvatarUrl(newAvatar);
        }

        int rows = userMapper.update(user);
        if (rows <= 0) {
            return Result.error("update failed");
        }

        cleanupAvatarIfChanged(originalAvatar, newAvatar);
        ensureNicknameFallback(user);
        return Result.success("update success", buildUserResponse(user));
    }

    @Override
    public Result<Void> changePassword(DataRequest dataRequest) {
        Map<String, Object> data = safeData(dataRequest);
        if (data.isEmpty()) {
            return Result.error("request payload is empty");
        }
        String username = stringValue(data.get("username"));
        String oldPassword = stringValue(data.get("oldPassword"));
        String newPassword = stringValue(data.get("newPassword"));
        if (!isValidUsername(username) || oldPassword == null || newPassword == null) {
            return Result.error("invalid username or password");
        }
        if (!isValidPassword(newPassword)) {
            return Result.error("new password must be at least 6 characters");
        }

        User user = userMapper.getByUsernameAndPassword(username, oldPassword);
        if (user == null) {
            return Result.error("current password is incorrect");
        }
        user.setPassword(newPassword);
        int rows = userMapper.update(user);
        if (rows <= 0) {
            return Result.error("change password failed");
        }
        return Result.success("password updated");
    }

    @Override
    public Result<Map<String, Object>> getUserStats(DataRequest dataRequest) {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalUsers", getTotalUserCount());
        stats.put("guestCount", getUserCountByRole("GUEST"));
        stats.put("restaurateurCount", getUserCountByRole("RESTAURATEUR"));
        stats.put("deliverymanCount", getUserCountByRole("DELIVERYMAN"));
        stats.put("adminCount", getUserCountByRole("ADMIN"));
        return Result.success(stats);
    }

    @Override
    public Result<Map<String, Object>> uploadAvatar(DataRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("file is empty");
        }
        try {
            String filename = FileUtil.saveFile(file, AVATAR_SUBDIR);
            Map<String, Object> payload = new HashMap<>();
            payload.put("filename", filename);
            payload.put("url", "/api/user/avatar/" + filename);
            Map<String, Object> data = safeData(request);
            if (data.containsKey("username")) {
                payload.put("username", stringValue(data.get("username")));
            }
            return Result.success("upload success", payload);
        } catch (IOException ex) {
            return Result.error("upload failed: " + ex.getMessage());
        }
    }

    @Override
    public Result<Map<String, Object>> getAvatar(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return Result.error("filename is required");
        }
        try {
            Path directory = AVATAR_ROOT.toAbsolutePath().normalize();
            Path filePath = directory.resolve(filename.trim()).normalize();
            if (!filePath.startsWith(directory) || !Files.exists(filePath)) {
                return Result.error("avatar not found");
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
            return Result.success("avatar loaded", payload);
        } catch (IOException ex) {
            return Result.error("read avatar failed: " + ex.getMessage());
        }
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

    private void handleRestaurateurRegistration(Map<String, Object> data, User newUser) {
        Restaurateur restaurateur = new Restaurateur();
        restaurateur.setUser(newUser);
        restaurateurMapper.insert(restaurateur);

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
        }
    }

    private void cleanupAvatarIfChanged(String originalAvatar, String newAvatar) {
        if (newAvatar == null || originalAvatar == null) {
            return;
        }
        if (originalAvatar.equals(newAvatar)) {
            return;
        }
        String prevFilename = extractFilename(originalAvatar);
        if (prevFilename != null) {
            try {
                FileUtil.deleteFile(prevFilename, AVATAR_SUBDIR);
            } catch (Exception ignored) {
            }
        }
    }

    private Map<String, Object> safeData(DataRequest request) {
        return RequestDataHelper.resolve(request);
    }

    private Integer intValueFromData(DataRequest request, String key) {
        Map<String, Object> data = safeData(request);
        Object value = data.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str.trim());
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
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value instanceof String str) {
            try {
                return Double.parseDouble(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
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

    private void attachToken(Map<String, Object> payload, JwtUtil.TokenDetails token) {
        if (payload == null || token == null) {
            return;
        }
        payload.put("token", token.getToken());
        payload.put("tokenType", "Bearer");
        if (token.getExpiresAt() != null) {
            payload.put("tokenExpiresAt", token.getExpiresAt().toString());
        }
        payload.put("tokenIssuedAt", token.getIssuedAt() != null ? token.getIssuedAt().toString() : null);
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

    private boolean isValidUsername(String username) {
        return username != null && USERNAME_PATTERN.matcher(username).matches();
    }

    private boolean isValidPassword(String password) {
        return password != null && PASSWORD_PATTERN.matcher(password).matches();
    }

    private void ensureNicknameFallback(User user) {
        if (user == null) {
            return;
        }
        if (user.getNickname() == null || user.getNickname().isEmpty()) {
            user.setNickname(user.getUsername());
        }
    }
}
