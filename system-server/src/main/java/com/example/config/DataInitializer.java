package com.example.config;

import com.example.entity.Deliveryman;
import com.example.entity.Restaurant;
import com.example.entity.Restaurateur;
import com.example.entity.User;
import com.example.entity.UserRole;
import com.example.mapper.DeliverymanMapper;
import com.example.mapper.RestaurantMapper;
import com.example.mapper.RestaurateurMapper;
import com.example.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

/**
 * Recreates core tables and seeds baseline accounts on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final RestaurateurMapper restaurateurMapper;
    private final DeliverymanMapper deliverymanMapper;
    private final RestaurantMapper restaurantMapper;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        resetTables();
        seedUsers();
        log.info("Database reset complete. Seed users inserted.");
    }

    private void resetTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        List<String> tables = Arrays.asList("restaurant", "restaurateur", "deliveryman", "user");
        for (String table : tables) {
            try {
                jdbcTemplate.execute("TRUNCATE TABLE `" + table + "`");
                log.info("Truncated table: {}", table);
            } catch (DataAccessException ex) {
                log.warn("Skipping truncate for table {}: {}", table, ex.getMessage());
            }
        }
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1");
    }

    private void seedUsers() {
        createAdmin();
        createGuest();
        createRestaurateur();
        createDeliveryman();
    }

    private void createGuest() {
        User guest = buildUser("guest", "guest123", UserRole.GUEST, "体验用户",
                "13600000000", "guest@example.com", "上海市静安区南京西路1601号");
        userMapper.insertUser(guest);
        log.info("Inserted guest user with id {}", guest.getId());
    }

    private void createAdmin() {
        User admin = buildUser("admin", "admin123", UserRole.ADMIN, "系统管理员",
                "13800000000", "admin@example.com", "上海市浦东新区世纪大道100号");
        userMapper.insertUser(admin);
        log.info("Inserted admin user with id {}", admin.getId());
    }

    private void createRestaurateur() {
    User restUser = buildUser("restaurateur", "restaurateur123", UserRole.RESTAURATEUR, "张三",
        "13900000000", "restaurateur@example.com", "上海市黄浦区南京东路300号");

        // avoid duplicate seed on restart
        if (userMapper.getByUsername(restUser.getUsername()) != null) {
            log.info("Restaurateur user '{}' already exists - skipping restaurateur seed", restUser.getUsername());
            return;
        }

        userMapper.insertUser(restUser);

        Restaurateur restaurateur = new Restaurateur();
        restaurateur.setUser(restUser);
        restaurateur.setIncome(0.0);
        restaurateurMapper.insert(restaurateur);

    Restaurant bundBistro = new Restaurant();
    bundBistro.setRestaurantName("外滩星光餐厅");
    bundBistro.setRestaurantAddress("上海市黄浦区中山东一路3号");
    bundBistro.setRestaurantImageUrl(null);
    bundBistro.setLng(121.490317);
    bundBistro.setLat(31.241701);
    bundBistro.setRestaurateur(restaurateur);
    restaurantMapper.insert(bundBistro);

    log.info("Inserted restaurateur user {} with restaurant {}", restUser.getId(), bundBistro.getId());
    }

    private void createDeliveryman() {
    User deliveryUser = buildUser("deliveryman", "deliveryman123", UserRole.DELIVERYMAN, "李四",
        "13700000000", "deliveryman@example.com", "上海市闵行区吴中路1588号");
        userMapper.insertUser(deliveryUser);

        Deliveryman deliveryman = new Deliveryman();
        deliveryman.setUser(deliveryUser);
        deliveryman.setIncome(3200.0);
        deliverymanMapper.insert(deliveryman);

        log.info("Inserted deliveryman user {}", deliveryUser.getId());
    }

    private User buildUser(String username, String password, UserRole role, String nickname,
                           String phone, String email, String address) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(password);
        user.setRole(role);
        user.setNickname(nickname);
        user.setPhone(phone);
        user.setEmail(email);
        user.setAddress(address);
        user.setAvatarUrl(null);
        return user;
    }
}
