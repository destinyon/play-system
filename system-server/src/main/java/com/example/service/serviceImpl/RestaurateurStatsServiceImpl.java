package com.example.service.serviceImpl;

import com.example.common.DataRequest;
import com.example.common.Result;
import com.example.service.RestaurateurStatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class RestaurateurStatsServiceImpl implements RestaurateurStatsService {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public Result getStats(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            
            if (restaurateurId == null) {
                return Result.error("餐厅ID不能为空");
            }

            // 查询总收入
            Double totalIncome = jdbcTemplate.queryForObject(
                "SELECT COALESCE(SUM(total_amount), 0) FROM order_info " +
                "WHERE restaurateur_id = ? AND status = 'COMPLETED'",
                Double.class, restaurateurId
            );

            // 查询待处理订单数
            Integer pendingOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_info WHERE restaurateur_id = ? AND status = 'PENDING'",
                Integer.class, restaurateurId
            );

            // 查询今日订单数
            LocalDateTime todayStart = LocalDate.now().atStartOfDay();
            Integer todayOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_info WHERE restaurateur_id = ? AND created_at >= ?",
                Integer.class, restaurateurId, todayStart
            );

            // 查询昨日订单数计算增长率
            LocalDateTime yesterdayStart = LocalDate.now().minusDays(1).atStartOfDay();
            Integer yesterdayOrders = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM order_info WHERE restaurateur_id = ? " +
                "AND created_at >= ? AND created_at < ?",
                Integer.class, restaurateurId, yesterdayStart, todayStart
            );
            
            int growth = 0;
            if (yesterdayOrders > 0) {
                growth = (int) (((double)(todayOrders - yesterdayOrders) / yesterdayOrders) * 100);
            } else if (todayOrders > 0) {
                growth = 100;
            }

            // 查询菜品数量
            Integer dishCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM menu_item WHERE restaurateur_id = ? AND is_deleted = 0",
                Integer.class, restaurateurId
            );

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalIncome", totalIncome != null ? totalIncome : 0.0);
            stats.put("pendingOrders", pendingOrders != null ? pendingOrders : 0);
            stats.put("todayOrders", todayOrders != null ? todayOrders : 0);
            stats.put("todayOrdersGrowth", growth);
            stats.put("dishCount", dishCount != null ? dishCount : 0);

            return Result.success(stats);
        } catch (Exception e) {
            return Result.error("查询统计数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result getMetrics(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            String from = (String) data.get("from");
            String to = (String) data.get("to");
            String granularity = data.get("granularity") != null ? (String) data.get("granularity") : "day";

            if (restaurateurId == null) {
                return Result.error("餐厅ID不能为空");
            }

            // 解析时间范围
            LocalDateTime fromDate = from != null ? LocalDate.parse(from).atStartOfDay() : LocalDate.now().minusDays(6).atStartOfDay();
            LocalDateTime toDate = to != null ? LocalDate.parse(to).atTime(23, 59, 59) : LocalDateTime.now();

            // 查询时序数据
            List<Map<String, Object>> timeSeriesData = new ArrayList<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            long daysBetween = java.time.Duration.between(fromDate, toDate).toDays() + 1;
            for (int i = 0; i < daysBetween; i++) {
                LocalDate date = fromDate.toLocalDate().plusDays(i);
                LocalDateTime dayStart = date.atStartOfDay();
                LocalDateTime dayEnd = date.atTime(23, 59, 59);

                // 查询当日收入
                Double income = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(total_amount), 0) FROM order_info " +
                    "WHERE restaurateur_id = ? AND status = 'COMPLETED' " +
                    "AND created_at >= ? AND created_at <= ?",
                    Double.class, restaurateurId, dayStart, dayEnd
                );

                // 查询当日订单数
                Integer orders = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM order_info " +
                    "WHERE restaurateur_id = ? AND created_at >= ? AND created_at <= ?",
                    Integer.class, restaurateurId, dayStart, dayEnd
                );

                // 查询当日菜品销量
                Integer dishes = jdbcTemplate.queryForObject(
                    "SELECT COALESCE(SUM(oi.quantity), 0) FROM order_item oi " +
                    "JOIN order_info o ON oi.order_id = o.id " +
                    "WHERE o.restaurateur_id = ? AND o.created_at >= ? AND o.created_at <= ?",
                    Integer.class, restaurateurId, dayStart, dayEnd
                );

                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("date", date.format(formatter));
                dataPoint.put("income", income != null ? income : 0.0);
                dataPoint.put("orders", orders != null ? orders : 0);
                dataPoint.put("dishes", dishes != null ? dishes : 0);
                timeSeriesData.add(dataPoint);
            }

            Map<String, Object> result = new HashMap<>();
            result.put("timeSeries", timeSeriesData);
            result.put("granularity", granularity);
            result.put("from", fromDate.format(formatter));
            result.put("to", toDate.format(formatter));

            return Result.success(result);
        } catch (Exception e) {
            return Result.error("查询统计数据失败: " + e.getMessage());
        }
    }

    @Override
    public Result getTopDishes(DataRequest request) {
        try {
            Map<String, Object> data = request.getData();
            Integer restaurateurId = getIntValue(data, "restaurateurId");
            Integer limit = data.get("limit") != null ? 
                (data.get("limit") instanceof Integer ? (Integer) data.get("limit") : Integer.parseInt(data.get("limit").toString())) 
                : 10;

            if (restaurateurId == null) {
                return Result.error("餐厅ID不能为空");
            }

            // 查询畅销菜品
            String sql = "SELECT oi.dish_name as name, " +
                        "SUM(oi.quantity) as salesCount, " +
                        "SUM(oi.unit_price * oi.quantity) as revenue " +
                        "FROM order_item oi " +
                        "JOIN order_info o ON oi.order_id = o.id " +
                        "WHERE o.restaurateur_id = ? AND o.status = 'COMPLETED' " +
                        "GROUP BY oi.dish_name " +
                        "ORDER BY salesCount DESC " +
                        "LIMIT ?";

            List<Map<String, Object>> topDishes = jdbcTemplate.queryForList(sql, restaurateurId, limit);

            return Result.success(topDishes);
        } catch (Exception e) {
            return Result.error("查询畅销菜品失败: " + e.getMessage());
        }
    }

    private Integer getIntValue(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Integer) {
            return (Integer) value;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return Integer.parseInt(value.toString());
    }
}
