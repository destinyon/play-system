package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.entity.Restaurant;
import com.example.entity.RestaurantReview;
import com.example.entity.RestaurantReviewLike;
import com.example.entity.Restaurateur;
import com.example.entity.User;
import com.example.mapper.RestaurantMapper;
import com.example.mapper.RestaurantReviewLikeMapper;
import com.example.mapper.RestaurantReviewMapper;
import com.example.mapper.RestaurateurMapper;
import com.example.mapper.UserMapper;
import com.example.service.RestaurantService;
import com.example.util.FileUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private static final String RESTAURANT_SUBDIR = "restaurants";

    private final RestaurantMapper restaurantMapper;
    private final RestaurateurMapper restaurateurMapper;
    private final UserMapper userMapper;
    private final RestaurantReviewMapper restaurantReviewMapper;
    private final RestaurantReviewLikeMapper restaurantReviewLikeMapper;

    @Override
    public Result uploadPhoto(DataRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("文件为空");
        }
        try {
            String filename = FileUtil.saveFile(file, RESTAURANT_SUBDIR);
            Map<String, Object> data = new HashMap<>();
            data.put("url", "/uploads/restaurants/" + filename);
            data.put("filename", filename);
            return Result.success(data);
        } catch (IOException ex) {
            return Result.error("上传失败: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public Result createOrUpdate(DataRequest request) {
        Map<String, Object> data = safeData(request);
        if (data == null) {
            return Result.error("缺少数据");
        }

        String name = stringValue(data.get("name"));
        String address = stringValue(data.get("address"));
        if (name == null || address == null) {
            return Result.error("餐馆名称和详细地址不能为空");
        }

        Double lng = doubleValue(data.get("lng"));
        Double lat = doubleValue(data.get("lat"));
        if (lng == null || lat == null) {
            return Result.error("经纬度不能为空");
        }

        Integer restaurateurId = resolveRestaurateurId(data);
        if (restaurateurId == null) {
            return Result.error("无法解析商家信息");
        }

        Restaurant restaurant = new Restaurant();
        restaurant.setRestaurantName(name);
        restaurant.setRestaurantAddress(address);
        restaurant.setDescription(stringValue(data.get("description")));
        restaurant.setRestaurantImageUrl(stringValue(data.get("photoUrl")));
        restaurant.setLng(lng);
        restaurant.setLat(lat);

        Restaurateur restaurateur = new Restaurateur();
        restaurateur.setId(restaurateurId);
        restaurant.setRestaurateur(restaurateur);

        Restaurant existed = restaurantMapper.getByRestaurateurId(restaurateurId);
        if (existed != null) {
            restaurant.setId(existed.getId());
            restaurantMapper.updateById(restaurant);
        } else {
            restaurantMapper.insert(restaurant);
        }
        return Result.success("操作成功", restaurant);
    }

    @Override
    public Result listAll() {
        List<Restaurant> restaurants = restaurantMapper.listAll();
        return Result.success(restaurants == null ? Collections.emptyList() : restaurants);
    }

    @Override
    public Result getProfile(DataRequest request) {
        Map<String, Object> data = safeData(request);
        String username = extractUsername(data);
        if (username == null) {
            return Result.error("缺少用户名");
        }

        User user = findUserByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }

        Restaurateur restaurateur = restaurateurMapper.getByUserId(user.getId());
        if (restaurateur == null) {
            return Result.error("用户不是商家或尚未完成商家认证");
        }

        Restaurant restaurant = restaurantMapper.getByRestaurateurId(restaurateur.getId());
        Map<String, Object> payload = new HashMap<>();
        payload.put("restaurant", buildRestaurantPayload(restaurant, restaurateur));

        if (restaurant != null && restaurant.getId() != null) {
            int reviewCount = restaurantReviewMapper.countByRestaurant(restaurant.getId());
            Double average = restaurantReviewMapper.averageRatingByRestaurant(restaurant.getId());
            Integer likeSum = restaurantReviewMapper.sumLikesByRestaurant(restaurant.getId());

            Map<String, Object> summary = new HashMap<>();
            summary.put("reviewCount", reviewCount);
            summary.put("avgRating", average == null ? 0.0 : roundHalfUp(average, 1));
            summary.put("likeCount", likeSum == null ? 0 : likeSum);
            payload.put("summary", summary);

            List<RestaurantReview> latest = restaurantReviewMapper.listByRestaurant(restaurant.getId(), 0, 1);
            if (latest != null && !latest.isEmpty()) {
                payload.put("highlight", buildReviewPayload(latest.get(0), null));
            }
        } else {
            Map<String, Object> summary = new HashMap<>();
            summary.put("reviewCount", 0);
            summary.put("avgRating", 0.0);
            summary.put("likeCount", 0);
            payload.put("summary", summary);
        }

        return Result.success(payload);
    }

    @Override
    @Transactional
    public Result saveProfile(DataRequest request) {
        Map<String, Object> payload = safeData(request);
        if (payload == null) {
            return Result.error("请求数据为空");
        }
        String username = extractUsername(payload);
        if (username == null) {
            return Result.error("缺少用户名");
        }

        String name = stringValue(payload.get("name"));
        String address = stringValue(payload.get("address"));
        Double lng = doubleValue(payload.get("lng"));
        Double lat = doubleValue(payload.get("lat"));

        if (name == null) {
            return Result.error("餐馆名称不能为空");
        }
        if (address == null) {
            return Result.error("餐馆地址不能为空");
        }
        if (lng == null || lat == null) {
            return Result.error("请在地图上选择餐馆位置");
        }

        User user = findUserByUsername(username);
        if (user == null) {
            return Result.error("用户不存在");
        }
        Restaurateur restaurateur = restaurateurMapper.getByUserId(user.getId());
        if (restaurateur == null) {
            return Result.error("用户不是商家或尚未完成商家认证");
        }

        Restaurant restaurant = restaurantMapper.getByRestaurateurId(restaurateur.getId());
        boolean isNew = restaurant == null || restaurant.getId() == null;
        if (isNew) {
            restaurant = new Restaurant();
            restaurant.setRestaurateur(restaurateur);
        }

        restaurant.setRestaurantName(name);
        restaurant.setRestaurantAddress(address);
        restaurant.setLng(lng);
        restaurant.setLat(lat);
        restaurant.setDescription(stringValue(payload.get("description")));
        restaurant.setRestaurantImageUrl(stringValue(payload.get("photoUrl")));

        if (isNew) {
            restaurantMapper.insert(restaurant);
        } else {
            restaurantMapper.updateById(restaurant);
        }

        return Result.success(buildRestaurantPayload(restaurant, restaurateur));
    }

    @Override
    public Result listReviews(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer restaurantId = data == null ? null : intValue(data.get("restaurantId"));
        if (restaurantId == null) {
            return Result.error("缺少餐馆ID");
        }

        Integer page = request != null && request.getPage() != null ? request.getPage() : (data == null ? null : intValue(data.get("page")));
        Integer size = request != null && request.getSize() != null ? request.getSize() : (data == null ? null : intValue(data.get("size")));
        Integer currentUserId = resolveUserId(data);

        int pageIndex = page == null || page < 1 ? 1 : page;
        int pageSize = size == null || size < 1 ? 10 : Math.min(size, 50);
        int offset = (pageIndex - 1) * pageSize;

        int total = restaurantReviewMapper.countByRestaurant(restaurantId);
        if (total == 0) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("items", Collections.emptyList());
            empty.put("page", pageIndex);
            empty.put("size", pageSize);
            empty.put("total", 0);
            return Result.success(empty);
        }

        List<RestaurantReview> reviews = restaurantReviewMapper.listByRestaurant(restaurantId, offset, pageSize);
        List<Map<String, Object>> items = new ArrayList<>();
        Set<Integer> likedReviewIds = new HashSet<>();
        if (currentUserId != null && reviews != null && !reviews.isEmpty()) {
            List<Integer> reviewIds = new ArrayList<>();
            for (RestaurantReview review : reviews) {
                if (review != null && review.getId() != null) {
                    reviewIds.add(review.getId());
                }
            }
            if (!reviewIds.isEmpty()) {
                List<Integer> liked = restaurantReviewLikeMapper.findLikedReviewIds(currentUserId, reviewIds);
                if (liked != null) {
                    likedReviewIds.addAll(liked);
                }
            }
        }

        if (reviews != null) {
            for (RestaurantReview review : reviews) {
                items.add(buildReviewPayload(review, likedReviewIds));
            }
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("items", items);
        payload.put("page", pageIndex);
        payload.put("size", pageSize);
        payload.put("total", total);
        return Result.success(payload);
    }

    @Override
    @Transactional
    public Result toggleReviewLike(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer reviewId = data == null ? null : intValue(data.get("reviewId"));
        Integer currentUserId = resolveUserId(data);
        if (reviewId == null || currentUserId == null) {
            return Result.error("缺少必要参数");
        }

        RestaurantReview review = restaurantReviewMapper.findById(reviewId);
        if (review == null || review.getId() == null) {
            return Result.error("评论不存在");
        }

        RestaurantReviewLike existed = restaurantReviewLikeMapper.findByReviewAndUser(reviewId, currentUserId);
        boolean liked;
        if (existed != null) {
            restaurantReviewLikeMapper.deleteById(existed.getId());
            if (review.getLikes() != null && review.getLikes() > 0) {
                restaurantReviewMapper.incrementLikes(reviewId, -1);
            }
            liked = false;
        } else {
            RestaurantReviewLike like = new RestaurantReviewLike();
            like.setReviewId(reviewId);
            like.setUserId(currentUserId);
            like.setCreatedAt(LocalDateTime.now());
            restaurantReviewLikeMapper.insert(like);
            restaurantReviewMapper.incrementLikes(reviewId, 1);
            liked = true;
        }

        RestaurantReview updated = restaurantReviewMapper.findById(reviewId);
        Map<String, Object> payload = buildReviewPayload(updated, null);
        payload.put("liked", liked);
        return Result.success(payload);
    }

    @Override
    @Transactional
    public Result createReview(DataRequest request) {
        Map<String, Object> data = safeData(request);
        if (data == null) {
            return Result.error("请求数据为空");
        }

        Integer restaurantId = intValue(data.get("restaurantId"));
        Integer userId = resolveUserId(data);
        Integer rating = intValue(data.get("rating"));
        Integer orderId = intValue(data.get("orderId"));
        String content = stringValue(data.get("content"));
        String detail = stringValue(data.get("detail"));

        if (restaurantId == null || userId == null) {
            return Result.error("缺少必要参数");
        }

        Restaurant restaurant = restaurantMapper.getById(restaurantId);
        if (restaurant == null) {
            return Result.error("餐馆不存在");
        }

        User user = userMapper.getById(userId);
        if (user == null) {
            return Result.error("用户不存在");
        }

        if (content == null || content.isEmpty()) {
            return Result.error("评论内容不能为空");
        }

        int rate = rating == null ? 5 : Math.max(1, Math.min(5, rating));

        RestaurantReview review = new RestaurantReview();
        review.setRestaurant(restaurant);
        review.setUser(user);
        review.setOrderId(orderId);
        review.setRating(rate);
        review.setContent(content);
        review.setDetail(detail);
        review.setLikes(0);
        review.setDeleted(false);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(review.getCreatedAt());

        restaurantReviewMapper.insert(review);
        return Result.success(buildReviewPayload(review, null));
    }

    private Map<String, Object> buildRestaurantPayload(Restaurant restaurant, Restaurateur restaurateur) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", restaurant == null ? null : restaurant.getId());
        payload.put("name", restaurant == null ? "" : optionalString(restaurant.getRestaurantName()));
        payload.put("address", restaurant == null ? "" : optionalString(restaurant.getRestaurantAddress()));
        payload.put("description", restaurant == null ? "" : optionalString(restaurant.getDescription()));
        payload.put("lng", restaurant == null ? null : restaurant.getLng());
        payload.put("lat", restaurant == null ? null : restaurant.getLat());
        payload.put("photoUrl", restaurant == null ? null : restaurant.getRestaurantImageUrl());
        payload.put("restaurateurId", restaurateur == null ? null : restaurateur.getId());
        return payload;
    }

    private Map<String, Object> buildReviewPayload(RestaurantReview review, Set<Integer> likedReviewIds) {
        if (review == null) {
            return Collections.emptyMap();
        }
        Map<String, Object> payload = new HashMap<>();
        payload.put("id", review.getId());
        payload.put("rating", review.getRating());
        payload.put("content", review.getContent());
        payload.put("detail", review.getDetail());
        payload.put("likes", review.getLikes());
        payload.put("createdAt", review.getCreatedAt());
        if (review.getUser() != null) {
            Map<String, Object> user = new HashMap<>();
            user.put("id", review.getUser().getId());
            user.put("username", review.getUser().getUsername());
            user.put("nickname", Optional.ofNullable(review.getUser().getNickname()).orElse(review.getUser().getUsername()));
            user.put("avatarUrl", review.getUser().getAvatarUrl());
            payload.put("user", user);
        }
        payload.put("liked", likedReviewIds != null && review.getId() != null && likedReviewIds.contains(review.getId()));
        if (review.getOrderId() != null) {
            payload.put("orderId", review.getOrderId());
        }
        return payload;
    }

    private Map<String, Object> safeData(DataRequest request) {
        return request == null ? null : request.getData();
    }

    private String extractUsername(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        String direct = stringValue(data.get("username"));
        if (direct != null) {
            return direct;
        }
        Object userObj = data.get("user");
        if (userObj instanceof Map) {
            Map<?, ?> userMap = (Map<?, ?>) userObj;
            return stringValue(userMap.get("username"));
        }
        return null;
    }

    private Integer resolveUserId(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Integer direct = intValue(data.get("userId"));
        if (direct != null) {
            return direct;
        }
        Object userObj = data.get("user");
        if (userObj instanceof Map) {
            Map<?, ?> userMap = (Map<?, ?>) userObj;
            Integer nestedId = intValue(userMap.get("id"));
            if (nestedId != null) {
                return nestedId;
            }
            String nestedUsername = stringValue(userMap.get("username"));
            if (nestedUsername != null) {
                User user = findUserByUsername(nestedUsername);
                return user == null ? null : user.getId();
            }
        }
        String username = extractUsername(data);
        User user = findUserByUsername(username);
        return user == null ? null : user.getId();
    }

    private Integer resolveRestaurateurId(Map<String, Object> data) {
        if (data == null) {
            return null;
        }
        Integer direct = intValue(data.get("restaurateurId"));
        if (direct != null) {
            return direct;
        }
        Object restObj = data.get("restaurateur");
        if (restObj instanceof Map) {
            Map<?, ?> restMap = (Map<?, ?>) restObj;
            Integer nestedId = intValue(restMap.get("id"));
            if (nestedId != null) {
                return nestedId;
            }
            String restUsername = stringValue(restMap.get("username"));
            if (restUsername != null) {
                return ensureRestaurateurId(restUsername);
            }
        }
        String username = extractUsername(data);
        if (username != null) {
            return ensureRestaurateurId(username);
        }
        return null;
    }

    private Integer ensureRestaurateurId(String username) {
        User user = findUserByUsername(username);
        if (user == null) {
            return null;
        }
        Restaurateur restaurateur = restaurateurMapper.getByUserId(user.getId());
        if (restaurateur == null) {
            Restaurateur created = new Restaurateur();
            created.setUser(user);
            restaurateurMapper.insert(created);
            restaurateur = created;
        }
        return restaurateur.getId();
    }

    private User findUserByUsername(String username) {
        if (username == null || username.trim().isEmpty()) {
            return null;
        }
        return userMapper.getByUsername(username.trim());
    }

    private Integer intValue(Object value) {
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

    private String stringValue(Object obj) {
        if (obj == null) {
            return null;
        }
        String str = obj.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private Double doubleValue(Object obj) {
        if (obj instanceof Number) {
            return ((Number) obj).doubleValue();
        }
        if (obj instanceof String) {
            try {
                return Double.valueOf(((String) obj).trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String optionalString(String value) {
        return value == null ? "" : value;
    }

    private double roundHalfUp(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }
}
