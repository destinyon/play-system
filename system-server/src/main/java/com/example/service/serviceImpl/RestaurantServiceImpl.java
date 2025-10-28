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
import com.example.security.RequestDataHelper;
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
import java.util.Set;

@Service
@RequiredArgsConstructor
public class RestaurantServiceImpl implements RestaurantService {

    private static final String RESTAURANT_SUBDIR = "restaurants";
    private static final int MAX_NAME_LENGTH = 120;
    private static final int MAX_ADDRESS_LENGTH = 255;
    private static final int MAX_DESCRIPTION_LENGTH = 2000;

    private final RestaurantMapper restaurantMapper;
    private final RestaurateurMapper restaurateurMapper;
    private final UserMapper userMapper;
    private final RestaurantReviewMapper restaurantReviewMapper;
    private final RestaurantReviewLikeMapper restaurantReviewLikeMapper;

    @Override
    public Result<Map<String, String>> uploadPhoto(DataRequest request, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return Result.error("file is empty");
        }
        try {
            String filename = FileUtil.saveFile(file, RESTAURANT_SUBDIR);
            Map<String, String> payload = new HashMap<>();
            payload.put("url", "/uploads/restaurants/" + filename);
            payload.put("filename", filename);
            return Result.success("upload success", payload);
        } catch (IOException ex) {
            return Result.error("upload failed: " + ex.getMessage());
        }
    }

    @Override
    @Transactional
    public Result<Restaurant> createOrUpdate(DataRequest request) {
        Map<String, Object> data = safeData(request);
        if (data == null) {
            return Result.error("missing payload");
        }

        ValidationResult validation = validateRestaurantPayload(data);
        if (!validation.errors.isEmpty()) {
            return Result.error(String.join("; ", validation.errors));
        }

        Integer restaurateurId = resolveRestaurateurId(data);
        if (restaurateurId == null) {
            return Result.error("unable to resolve restaurateur");
        }

        Restaurant restaurant = buildRestaurantFromPayload(data, validation.longitude, validation.latitude);
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
        return Result.success("saved", restaurant);
    }

    @Override
    public Result<List<Restaurant>> listAll(DataRequest request) {
        List<Restaurant> restaurants = restaurantMapper.listAll();
        if (restaurants == null) {
            restaurants = Collections.emptyList();
        }
        return Result.success(restaurants);
    }

    @Override
    public Result<Map<String, Object>> getProfile(DataRequest request) {
        Map<String, Object> data = safeData(request);
        String username = extractUsername(data);
        if (username == null) {
            return Result.error("missing username");
        }

        User user = findUserByUsername(username);
        if (user == null) {
            return Result.error("user not found");
        }

        Restaurateur restaurateur = restaurateurMapper.getByUserId(user.getId());
        if (restaurateur == null) {
            return Result.error("restaurateur profile not found");
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
    public Result<Restaurant> saveProfile(DataRequest request) {
        Map<String, Object> data = safeData(request);
        if (data == null) {
            return Result.error("missing payload");
        }
        String username = extractUsername(data);
        if (username == null) {
            return Result.error("missing username");
        }

        ValidationResult validation = validateRestaurantPayload(data);
        if (!validation.errors.isEmpty()) {
            return Result.error(String.join("; ", validation.errors));
        }

        User user = findUserByUsername(username);
        if (user == null) {
            return Result.error("user not found");
        }
        Restaurateur restaurateur = restaurateurMapper.getByUserId(user.getId());
        if (restaurateur == null) {
            return Result.error("restaurateur profile not found");
        }

        Restaurant restaurant = restaurantMapper.getByRestaurateurId(restaurateur.getId());
        boolean isNew = restaurant == null || restaurant.getId() == null;
        if (isNew) {
            restaurant = new Restaurant();
            restaurant.setRestaurateur(restaurateur);
        }
        applyRestaurantDetails(restaurant, data, validation.longitude, validation.latitude);

        if (isNew) {
            restaurantMapper.insert(restaurant);
        } else {
            restaurantMapper.updateById(restaurant);
        }
        return Result.success("saved", restaurant);
    }

    @Override
    public Result<Map<String, Object>> listReviews(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer restaurantId = data == null ? null : intValue(data.get("restaurantId"));
        if (restaurantId == null) {
            return Result.error("missing restaurantId");
        }

        Integer page = request != null && request.getPage() != null ? request.getPage() : intValue(data.get("page"));
        Integer size = request != null && request.getSize() != null ? request.getSize() : intValue(data.get("size"));
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
    public Result<Map<String, Object>> toggleReviewLike(DataRequest request) {
        Map<String, Object> data = safeData(request);
        Integer reviewId = data == null ? null : intValue(data.get("reviewId"));
        Integer currentUserId = resolveUserId(data);
        if (reviewId == null || currentUserId == null) {
            return Result.error("missing required parameters");
        }

        RestaurantReview review = restaurantReviewMapper.findById(reviewId);
        if (review == null || review.getId() == null) {
            return Result.error("review not found");
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

        RestaurantReview refreshed = restaurantReviewMapper.findById(reviewId);
        Map<String, Object> payload = buildReviewPayload(refreshed, Collections.singleton(currentUserId));
        payload.put("liked", liked);
        return Result.success(payload);
    }

    @Override
    @Transactional
    public Result<Map<String, Object>> createReview(DataRequest request) {
        Map<String, Object> data = safeData(request);
        if (data == null) {
            return Result.error("missing payload");
        }

        Integer restaurantId = intValue(data.get("restaurantId"));
        Integer userId = resolveUserId(data);
        Integer rating = intValue(data.get("rating"));
        String content = stringValue(data.get("content"));
        if (restaurantId == null || userId == null || rating == null || content == null) {
            return Result.error("missing required fields");
        }
        if (rating < 1 || rating > 5) {
            return Result.error("rating must be between 1 and 5");
        }

        RestaurantReview review = new RestaurantReview();
        review.setRestaurant(new Restaurant());
        review.getRestaurant().setId(restaurantId);

        User author = userMapper.getById(userId);
        if (author == null) {
            return Result.error("user not found");
        }
        review.setUser(author);
        review.setOrderId(intValue(data.get("orderId")));
        review.setRating(rating);
        review.setContent(content);
        review.setDetail(stringValue(data.get("detail")));
        review.setCreatedAt(LocalDateTime.now());
        review.setLikes(0);
        review.setDeleted(Boolean.FALSE);

        restaurantReviewMapper.insert(review);
        Map<String, Object> payload = buildReviewPayload(review, Collections.singleton(userId));
        return Result.success("created", payload);
    }

    private ValidationResult validateRestaurantPayload(Map<String, Object> data) {
        ValidationResult result = new ValidationResult();
        String name = stringValue(data.get("name"));
        String address = stringValue(data.get("address"));
        String description = stringValue(data.get("description"));
        Double lng = doubleValue(data.get("lng"));
        Double lat = doubleValue(data.get("lat"));

        if (name == null || name.isEmpty()) {
            result.errors.add("name is required");
        } else if (name.length() > MAX_NAME_LENGTH) {
            result.errors.add("name length exceeds " + MAX_NAME_LENGTH);
        }

        if (address == null || address.isEmpty()) {
            result.errors.add("address is required");
        } else if (address.length() > MAX_ADDRESS_LENGTH) {
            result.errors.add("address length exceeds " + MAX_ADDRESS_LENGTH);
        }

        if (description != null && description.length() > MAX_DESCRIPTION_LENGTH) {
            result.errors.add("description length exceeds " + MAX_DESCRIPTION_LENGTH);
        }

        if (lng == null || lat == null) {
            result.errors.add("location is required");
        } else {
            if (Math.abs(lng) > 180) {
                result.errors.add("longitude is invalid");
            } else {
                result.longitude = lng;
            }
            if (Math.abs(lat) > 90) {
                result.errors.add("latitude is invalid");
            } else {
                result.latitude = lat;
            }
        }
        return result;
    }

    private Restaurant buildRestaurantFromPayload(Map<String, Object> data, Double lng, Double lat) {
        Restaurant restaurant = new Restaurant();
        applyRestaurantDetails(restaurant, data, lng, lat);
        return restaurant;
    }

    private void applyRestaurantDetails(Restaurant restaurant, Map<String, Object> data, Double lng, Double lat) {
        restaurant.setRestaurantName(stringValue(data.get("name")));
        restaurant.setRestaurantAddress(stringValue(data.get("address")));
        restaurant.setDescription(truncate(stringValue(data.get("description")), MAX_DESCRIPTION_LENGTH));
        restaurant.setRestaurantImageUrl(stringValue(data.get("photoUrl")));
        restaurant.setLng(lng);
        restaurant.setLat(lat);
    }

    private Map<String, Object> buildRestaurantPayload(Restaurant restaurant, Restaurateur restaurateur) {
        Map<String, Object> payload = new HashMap<>();
        if (restaurant == null) {
            payload.put("id", null);
            payload.put("name", "");
            payload.put("address", "");
            payload.put("description", "");
            payload.put("photoUrl", "");
            payload.put("lng", null);
            payload.put("lat", null);
        } else {
            payload.put("id", restaurant.getId());
            payload.put("name", optionalString(restaurant.getRestaurantName()));
            payload.put("address", optionalString(restaurant.getRestaurantAddress()));
            payload.put("description", optionalString(restaurant.getDescription()));
            payload.put("photoUrl", optionalString(restaurant.getRestaurantImageUrl()));
            payload.put("lng", restaurant.getLng());
            payload.put("lat", restaurant.getLat());
        }
        if (restaurateur != null && restaurateur.getUser() != null) {
            Map<String, Object> owner = new HashMap<>();
            owner.put("id", restaurateur.getUser().getId());
            owner.put("username", restaurateur.getUser().getUsername());
            payload.put("owner", owner);
        }
        return payload;
    }

    private Map<String, Object> buildReviewPayload(RestaurantReview review, Set<Integer> likedReviewIds) {
        Map<String, Object> payload = new HashMap<>();
        if (review == null) {
            return payload;
        }
        payload.put("id", review.getId());
        payload.put("rating", review.getRating());
        payload.put("content", optionalString(review.getContent()));
        payload.put("detail", optionalString(review.getDetail()));
        payload.put("likes", review.getLikes() == null ? 0 : review.getLikes());
        payload.put("orderId", review.getOrderId());
        payload.put("createdAt", review.getCreatedAt());
        payload.put("liked", likedReviewIds != null && review.getId() != null && likedReviewIds.contains(review.getId()));

        Map<String, Object> userPayload = new HashMap<>();
        if (review.getUser() != null) {
            userPayload.put("id", review.getUser().getId());
            userPayload.put("username", review.getUser().getUsername());
            userPayload.put("nickname", review.getUser().getNickname());
            userPayload.put("avatarUrl", review.getUser().getAvatarUrl());
        }
        payload.put("user", userPayload);
        return payload;
    }

    private Map<String, Object> safeData(DataRequest request) {
        return RequestDataHelper.resolve(request);
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
        if (userObj instanceof Map<?, ?> userMap) {
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
        if (userObj instanceof Map<?, ?> userMap) {
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
        if (restObj instanceof Map<?, ?> restMap) {
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

    private String stringValue(Object obj) {
        if (obj == null) {
            return null;
        }
        String str = obj.toString().trim();
        return str.isEmpty() ? null : str;
    }

    private Double doubleValue(Object obj) {
        if (obj instanceof Number number) {
            return number.doubleValue();
        }
        if (obj instanceof String str) {
            try {
                return Double.valueOf(str.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String optionalString(String value) {
        return value == null ? "" : value;
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private double roundHalfUp(double value, int scale) {
        double factor = Math.pow(10, scale);
        return Math.round(value * factor) / factor;
    }

    private static class ValidationResult {
        final List<String> errors = new ArrayList<>();
        Double longitude;
        Double latitude;
    }
}
