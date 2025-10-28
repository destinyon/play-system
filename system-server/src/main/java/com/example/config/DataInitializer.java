package com.example.config;

import com.example.entity.Deliveryman;
import com.example.entity.ChatSession;
import com.example.entity.ChatMessage;
import com.example.entity.MenuItem;
import com.example.entity.OrderInfo;
import com.example.entity.Restaurant;
import com.example.entity.RestaurantReview;
import com.example.entity.Restaurateur;
import com.example.entity.User;
import com.example.entity.UserRole;
import com.example.mapper.DeliverymanMapper;
import com.example.mapper.ChatSessionMapper;
import com.example.mapper.ChatMessageMapper;
import com.example.mapper.MenuItemMapper;
import com.example.mapper.RestaurantMapper;
import com.example.mapper.RestaurantReviewMapper;
import com.example.mapper.RestaurateurMapper;
import com.example.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.lang.NonNull;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Recreates core tables and seeds baseline accounts on application startup.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@DependsOn("entityManagerFactory")
@Order(Ordered.LOWEST_PRECEDENCE)
public class DataInitializer implements ApplicationListener<ApplicationReadyEvent> {

    private final JdbcTemplate jdbcTemplate;
    private final UserMapper userMapper;
    private final RestaurateurMapper restaurateurMapper;
    private final DeliverymanMapper deliverymanMapper;
    private final RestaurantMapper restaurantMapper;
    private final RestaurantReviewMapper restaurantReviewMapper;
    private final MenuItemMapper menuItemMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Override
    @Transactional
    public void onApplicationEvent(@NonNull ApplicationReadyEvent event) {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }
        prepareSchema();
        resetTables();
        seedUsers();
        seedRestaurantReviews();
        seedMenuItems();
        seedSampleOrders();
        seedChatData();
    log.info("Database reset complete. Seed users, menu items, and sample orders inserted.");
    }

    private void prepareSchema() {
        ensureColumnWithCheck("restaurant", "description", "ALTER TABLE restaurant ADD COLUMN description TEXT NULL");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS chat_session (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "session_key VARCHAR(128) NOT NULL UNIQUE," +
                "restaurateur_id INT NOT NULL," +
                "peer_user_id INT NOT NULL," +
                "peer_role VARCHAR(32) NOT NULL," +
                "order_id INT NULL," +
                "context_type VARCHAR(32) NULL," +
                "context_ref INT NULL," +
                "title VARCHAR(255) NULL," +
                "last_message_preview VARCHAR(255) NULL," +
                "last_message_time DATETIME NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "KEY idx_chat_session_restaurateur (restaurateur_id)," +
                "KEY idx_chat_session_peer (peer_user_id, peer_role)," +
                "KEY idx_chat_session_order (order_id)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS chat_message (" +
            "id INT AUTO_INCREMENT PRIMARY KEY," +
            "session_id INT NOT NULL," +
            "order_id INT NULL," +
            "context_type VARCHAR(32) NULL," +
            "context_ref INT NULL," +
            "sender_id INT NOT NULL," +
            "sender_role VARCHAR(32) NOT NULL," +
            "receiver_id INT NOT NULL," +
            "receiver_role VARCHAR(32) NOT NULL," +
            "content TEXT NOT NULL," +
            "read_flag TINYINT(1) NOT NULL DEFAULT 0," +
            "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
            "KEY idx_msg_session (session_id, id)," +
            "KEY idx_msg_receiver (receiver_role, receiver_id, read_flag)" +
            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
        ensureColumnWithCheck("chat_message", "session_id", "ALTER TABLE chat_message ADD COLUMN session_id INT NULL AFTER id");
        ensureColumnWithCheck("chat_message", "context_type", "ALTER TABLE chat_message ADD COLUMN context_type VARCHAR(32) NULL");
        ensureColumnWithCheck("chat_message", "context_ref", "ALTER TABLE chat_message ADD COLUMN context_ref INT NULL");
        ensureColumnWithCheck("chat_message", "read_flag", "ALTER TABLE chat_message ADD COLUMN read_flag TINYINT(1) NOT NULL DEFAULT 0");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS menu_item (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "restaurateur_id INT NOT NULL," +
                "name VARCHAR(128) NOT NULL," +
                "category VARCHAR(64)," +
                "price DECIMAL(10,2) NOT NULL," +
                "image_url VARCHAR(255)," +
                "description TEXT," +
                "status VARCHAR(16) NOT NULL DEFAULT 'ON_SHELF'," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "KEY idx_menu_restaurateur (restaurateur_id, is_deleted)," +
                "KEY idx_menu_category (category)" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS order_item (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "order_id INT NOT NULL," +
                "dish_id INT NULL," +
                "dish_name VARCHAR(128) NOT NULL," +
                "dish_image VARCHAR(255)," +
                "unit_price DECIMAL(10,2) NOT NULL," +
                "quantity INT NOT NULL DEFAULT 1," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "CONSTRAINT fk_order_item_order FOREIGN KEY (order_id) REFERENCES order_info(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        ensureColumnWithCheck("order_info", "delivery_address", "ALTER TABLE order_info ADD COLUMN delivery_address VARCHAR(255) NULL");
        ensureColumnWithCheck("order_info", "total_amount", "ALTER TABLE order_info ADD COLUMN total_amount DECIMAL(10,2) NOT NULL DEFAULT 0");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS restaurant_review (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "restaurant_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "order_id INT NULL," +
                "rating INT NOT NULL DEFAULT 5," +
                "content TEXT," +
                "detail TEXT," +
                "likes INT NOT NULL DEFAULT 0," +
                "is_deleted TINYINT(1) NOT NULL DEFAULT 0," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP," +
                "CONSTRAINT fk_rr_restaurant FOREIGN KEY (restaurant_id) REFERENCES restaurant(id) ON DELETE CASCADE," +
                "CONSTRAINT fk_rr_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");

        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS restaurant_review_like (" +
                "id INT AUTO_INCREMENT PRIMARY KEY," +
                "review_id INT NOT NULL," +
                "user_id INT NOT NULL," +
                "created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP," +
                "UNIQUE KEY uk_review_user (review_id, user_id)," +
                "CONSTRAINT fk_rrl_review FOREIGN KEY (review_id) REFERENCES restaurant_review(id) ON DELETE CASCADE," +
                "CONSTRAINT fk_rrl_user FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;");
    }

    private void resetTables() {
        jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0");
        List<String> tables = Arrays.asList(
                "order_item",
                "order_info",
            "menu_item",
                "chat_message",
                "chat_session",
                "restaurant_review_like",
                "restaurant_review",
                "restaurant",
                "restaurateur",
                "deliveryman",
                "user"
        );
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
        restaurateur.setUserId(restUser.getId());
        restaurateur.setIncome(0.0);
        restaurateurMapper.insert(restaurateur);

        Restaurant bundBistro = new Restaurant();
        bundBistro.setRestaurantName("外滩星光餐厅");
        bundBistro.setRestaurantAddress("上海市黄浦区中山东一路3号");
        bundBistro.setRestaurantImageUrl(null);
        bundBistro.setDescription("主打沪上融合创意菜，俯瞰外滩江景，提供中英文双语菜单和精品酒单。");
        bundBistro.setLng(121.490317);
        bundBistro.setLat(31.241701);
        bundBistro.setRestaurateurId(restaurateur.getId());
        restaurantMapper.insert(bundBistro);

        log.info("Inserted restaurateur user {} with restaurant {}", restUser.getId(), bundBistro.getId());
    }

    private void createDeliveryman() {
        User deliveryUser = buildUser("deliveryman", "deliveryman123", UserRole.DELIVERYMAN, "李四",
                "13700000000", "deliveryman@example.com", "上海市闵行区吴中路1588号");
        userMapper.insertUser(deliveryUser);

        Deliveryman deliveryman = new Deliveryman();
        deliveryman.setUserId(deliveryUser.getId());
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

    private void seedRestaurantReviews() {
        List<Restaurant> restaurants = restaurantMapper.listAll();
        if (restaurants == null || restaurants.isEmpty()) {
            return;
        }
        Restaurant restaurant = restaurants.get(0);
        User guest = userMapper.getByUsername("guest");
        User admin = userMapper.getByUsername("admin");
        if (guest == null || admin == null) {
            return;
        }

        RestaurantReview highlight = new RestaurantReview();
        highlight.setRestaurant(restaurant);
        highlight.setUser(guest);
        highlight.setOrderId(null);
        highlight.setRating(5);
        highlight.setContent("环境优雅，菜品精致，推荐他们家的玫瑰烤鸭和沪式冷盘组合，服务也很周到！");
        highlight.setDetail("就餐日期：" + LocalDateTime.now().minusDays(2).toLocalDate() + "\n两人套餐，含前菜、主菜、甜品和茶点。上菜节奏恰到好处，窗边靠江的位置非常适合约会。\n推荐菜：玫瑰烤鸭、酥皮小牛肉、桂花糯米藕。");
        highlight.setLikes(8);
        highlight.setDeleted(false);
        highlight.setCreatedAt(LocalDateTime.now().minusDays(2));
        highlight.setUpdatedAt(highlight.getCreatedAt());
        restaurantReviewMapper.insert(highlight);

        RestaurantReview feedback = new RestaurantReview();
        feedback.setRestaurant(restaurant);
        feedback.setUser(admin);
        feedback.setOrderId(null);
        feedback.setRating(4);
        feedback.setContent("景色一流，菜品有亮点但份量偏少，需提前预约热门座位。");
        feedback.setDetail("工作日晚餐体验。\n优点：江景视野无敌，服务人员专业且亲切。\n建议：菜品份量稍小，可以增加加菜选项；甜品可以更换为时令水果。\n总体而言值得推荐，会再访。");
        feedback.setLikes(3);
        feedback.setDeleted(false);
        feedback.setCreatedAt(LocalDateTime.now().minusDays(5));
        feedback.setUpdatedAt(feedback.getCreatedAt());
        restaurantReviewMapper.insert(feedback);
    }

    private void seedMenuItems() {
        User restaurateurUser = userMapper.getByUsername("restaurateur");
        if (restaurateurUser == null) {
            return;
        }
        Restaurateur restaurateur = restaurateurMapper.getByUserId(restaurateurUser.getId());
        if (restaurateur == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        
        // 热菜系列
        MenuItem spicyFish = new MenuItem();
        spicyFish.setRestaurateurId(restaurateur.getId());
        spicyFish.setName("川味水煮鱼");
        spicyFish.setCategory("热菜");
        spicyFish.setPrice(new java.math.BigDecimal("68.00"));
        spicyFish.setImageUrl("/uploads/dishes/川味水煮鱼1.png");
        spicyFish.setDescription("精选花椒与郫县豆瓣酱烹制，鲜香麻辣，极具四川风味。");
        spicyFish.setStatus("ON_SHELF");
        spicyFish.setDeleted(false);
        spicyFish.setCreatedAt(now.minusDays(3));
        spicyFish.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(spicyFish);

        MenuItem steak = new MenuItem();
        steak.setRestaurateurId(restaurateur.getId());
        steak.setName("外滩牛排");
        steak.setCategory("热菜");
        steak.setPrice(new java.math.BigDecimal("88.00"));
        steak.setImageUrl("/uploads/dishes/外滩牛排2.png");
        steak.setDescription("精选澳洲和牛，配黑椒汁或蘑菇汁，肉质鲜嫩多汁。");
        steak.setStatus("ON_SHELF");
        steak.setDeleted(false);
        steak.setCreatedAt(now.minusDays(4));
        steak.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(steak);

        MenuItem duck = new MenuItem();
        duck.setRestaurateurId(restaurateur.getId());
        duck.setName("玫瑰烤鸭");
        duck.setCategory("热菜");
        duck.setPrice(new java.math.BigDecimal("128.00"));
        duck.setImageUrl("/uploads/dishes/玫瑰烤鸭3.png");
        duck.setDescription("创意烤鸭，玫瑰花瓣与果木香结合,配荷叶饼和甜面酱。");
        duck.setStatus("ON_SHELF");
        duck.setDeleted(false);
        duck.setCreatedAt(now.minusDays(6));
        duck.setUpdatedAt(now.minusDays(3));
        saveOrUpdateMenuItem(duck);

        MenuItem kungPao = new MenuItem();
        kungPao.setRestaurateurId(restaurateur.getId());
        kungPao.setName("宫保鸡丁");
        kungPao.setCategory("热菜");
        kungPao.setPrice(new java.math.BigDecimal("48.00"));
        kungPao.setImageUrl("/uploads/dishes/宫保鸡丁4.png");
        kungPao.setDescription("经典川菜，鸡肉嫩滑，花生香脆，酸甜微辣，回味无穷。");
        kungPao.setStatus("ON_SHELF");
        kungPao.setDeleted(false);
        kungPao.setCreatedAt(now.minusDays(5));
        kungPao.setUpdatedAt(now.minusDays(2));
        saveOrUpdateMenuItem(kungPao);

        MenuItem braised = new MenuItem();
        braised.setRestaurateurId(restaurateur.getId());
        braised.setName("红烧肉");
        braised.setCategory("热菜");
        braised.setPrice(new java.math.BigDecimal("58.00"));
        braised.setImageUrl("/uploads/dishes/红烧肉5.png");
        braised.setDescription("肥而不腻，瘦而不柴，酱色红润，入口即化，配白米饭绝配。");
        braised.setStatus("ON_SHELF");
        braised.setDeleted(false);
        braised.setCreatedAt(now.minusDays(4));
        braised.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(braised);

        // 凉菜系列
        MenuItem salad = new MenuItem();
        salad.setRestaurateurId(restaurateur.getId());
        salad.setName("清新金枪鱼沙拉");
        salad.setCategory("凉菜");
        salad.setPrice(new java.math.BigDecimal("36.00"));
        salad.setImageUrl("/uploads/dishes/清新金枪鱼沙拉6.png");
        salad.setDescription("低脂金枪鱼搭配时蔬与特调油醋，适合轻食人群。");
        salad.setStatus("ON_SHELF");
        salad.setDeleted(false);
        salad.setCreatedAt(now.minusDays(2));
        salad.setUpdatedAt(now.minusHours(12));
        saveOrUpdateMenuItem(salad);

        MenuItem cucumber = new MenuItem();
        cucumber.setRestaurateurId(restaurateur.getId());
        cucumber.setName("凉拌黄瓜");
        cucumber.setCategory("凉菜");
        cucumber.setPrice(new java.math.BigDecimal("15.00"));
        cucumber.setImageUrl("/uploads/dishes/凉拌黄瓜7.png");
        cucumber.setDescription("新鲜黄瓜拍碎，蒜蓉香醋调味，清脆爽口，开胃解腻。");
        cucumber.setStatus("ON_SHELF");
        cucumber.setDeleted(false);
        cucumber.setCreatedAt(now.minusDays(3));
        cucumber.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(cucumber);

        MenuItem tofu = new MenuItem();
        tofu.setRestaurateurId(restaurateur.getId());
        tofu.setName("麻辣豆腐丝");
        tofu.setCategory("凉菜");
        tofu.setPrice(new java.math.BigDecimal("18.00"));
        tofu.setImageUrl("/uploads/dishes/麻辣豆腐丝8.png");
        tofu.setDescription("豆腐干切丝，麻辣鲜香，口感筋道，佐酒小菜的首选。");
        tofu.setStatus("OFF_SHELF");
        tofu.setDeleted(false);
        tofu.setCreatedAt(now.minusDays(4));
        tofu.setUpdatedAt(now.minusDays(2));
        saveOrUpdateMenuItem(tofu);

        // 甜品系列
        MenuItem dessert = new MenuItem();
        dessert.setRestaurateurId(restaurateur.getId());
        dessert.setName("桂花酒酿圆子");
        dessert.setCategory("甜品");
        dessert.setPrice(new java.math.BigDecimal("28.00"));
        dessert.setImageUrl("/uploads/dishes/桂花酒酿圆子9.png");
        dessert.setDescription("糯米圆子搭配自制桂花酒酿，温润香甜，是饭后暖心之选。");
        dessert.setStatus("ON_SHELF");
        dessert.setDeleted(false);
        dessert.setCreatedAt(now.minusDays(5));
        dessert.setUpdatedAt(now.minusDays(2));
        saveOrUpdateMenuItem(dessert);

        MenuItem mango = new MenuItem();
        mango.setRestaurateurId(restaurateur.getId());
        mango.setName("芒果班戟");
        mango.setCategory("甜品");
        mango.setPrice(new java.math.BigDecimal("32.00"));
        mango.setImageUrl("/uploads/dishes/芒果班戟10.png");
        mango.setDescription("薄如蝉翼的班戟皮包裹新鲜芒果与淡奶油，甜而不腻，冰爽宜人。");
        mango.setStatus("ON_SHELF");
        mango.setDeleted(false);
        mango.setCreatedAt(now.minusDays(3));
        mango.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(mango);

        MenuItem tiramisu = new MenuItem();
        tiramisu.setRestaurateurId(restaurateur.getId());
        tiramisu.setName("提拉米苏");
        tiramisu.setCategory("甜品");
        tiramisu.setPrice(new java.math.BigDecimal("38.00"));
        tiramisu.setImageUrl("/uploads/dishes/提拉米苏11.png");
        tiramisu.setDescription("意式经典甜品，马斯卡彭奶酪配咖啡酒手指饼，口感绵密，回味悠长。");
        tiramisu.setStatus("ON_SHELF");
        tiramisu.setDeleted(false);
        tiramisu.setCreatedAt(now.minusDays(4));
        tiramisu.setUpdatedAt(now.minusDays(2));
        saveOrUpdateMenuItem(tiramisu);

        // 饮品系列
        MenuItem tea = new MenuItem();
        tea.setRestaurateurId(restaurateur.getId());
        tea.setName("店制冰柠茶");
        tea.setCategory("饮品");
        tea.setPrice(new java.math.BigDecimal("20.00"));
        tea.setImageUrl("/uploads/dishes/店制冰柠茶12.png");
        tea.setDescription("新鲜柠檬手打，清爽解腻，冷热皆宜。");
        tea.setStatus("ON_SHELF");
        tea.setDeleted(false);
        tea.setCreatedAt(now.minusDays(3));
        tea.setUpdatedAt(now.minusHours(6));
        saveOrUpdateMenuItem(tea);

        MenuItem orange = new MenuItem();
        orange.setRestaurateurId(restaurateur.getId());
        orange.setName("鲜榨橙汁");
        orange.setCategory("饮品");
        orange.setPrice(new java.math.BigDecimal("22.00"));
        orange.setImageUrl("/uploads/dishes/鲜榨橙汁13.png");
        orange.setDescription("现点现榨，100%纯鲜橙汁，富含维C，健康美味。");
        orange.setStatus("ON_SHELF");
        orange.setDeleted(false);
        orange.setCreatedAt(now.minusDays(4));
        orange.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(orange);

        MenuItem milk = new MenuItem();
        milk.setRestaurateurId(restaurateur.getId());
        milk.setName("港式奶茶");
        milk.setCategory("饮品");
        milk.setPrice(new java.math.BigDecimal("18.00"));
        milk.setImageUrl("/uploads/dishes/港式奶茶14.png");
        milk.setDescription("丝袜奶茶配方，茶香浓郁，奶香醇厚，冷热皆可，经典茶餐厅味道。");
        milk.setStatus("ON_SHELF");
        milk.setDeleted(false);
        milk.setCreatedAt(now.minusDays(5));
        milk.setUpdatedAt(now.minusDays(2));
        saveOrUpdateMenuItem(milk);

        // 主食系列
        MenuItem rice = new MenuItem();
        rice.setRestaurateurId(restaurateur.getId());
        rice.setName("黄金蛋炒饭");
        rice.setCategory("主食");
        rice.setPrice(new java.math.BigDecimal("18.00"));
        rice.setImageUrl("/uploads/dishes/黄金蛋炒饭15.png");
        rice.setDescription("粒粒分明的扬州炒饭，蛋液裹饭，色泽金黄，鲜香可口。");
        rice.setStatus("ON_SHELF");
        rice.setDeleted(false);
        rice.setCreatedAt(now.minusDays(4));
        rice.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(rice);

        MenuItem noodles = new MenuItem();
        noodles.setRestaurateurId(restaurateur.getId());
        noodles.setName("招牌手擀面");
        noodles.setCategory("主食");
        noodles.setPrice(new java.math.BigDecimal("22.00"));
        noodles.setImageUrl("/uploads/dishes/招牌手擀面16.png");
        noodles.setDescription("厨师现场手擀，劲道爽滑，可配红烧肉汤或番茄鸡蛋汤。");
        noodles.setStatus("ON_SHELF");
        noodles.setDeleted(false);
        noodles.setCreatedAt(now.minusDays(5));
        noodles.setUpdatedAt(now.minusDays(2));
        saveOrUpdateMenuItem(noodles);

        MenuItem yangzhou = new MenuItem();
        yangzhou.setRestaurateurId(restaurateur.getId());
        yangzhou.setName("扬州炒饭");
        yangzhou.setCategory("主食");
        yangzhou.setPrice(new java.math.BigDecimal("25.00"));
        yangzhou.setImageUrl("/uploads/dishes/扬州炒饭17.png");
        yangzhou.setDescription("虾仁、火腿、青豆等配料丰富，色香味俱全，江南名菜。");
        yangzhou.setStatus("ON_SHELF");
        yangzhou.setDeleted(false);
        yangzhou.setCreatedAt(now.minusDays(3));
        yangzhou.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(yangzhou);

        MenuItem scallion = new MenuItem();
        scallion.setRestaurateurId(restaurateur.getId());
        scallion.setName("葱油拌面");
        scallion.setCategory("主食");
        scallion.setPrice(new java.math.BigDecimal("16.00"));
        scallion.setImageUrl("/uploads/dishes/葱油拌面18.png");
        scallion.setDescription("上海经典小吃，葱油熬制香浓，拌面简单却回味悠长。");
        scallion.setStatus("ON_SHELF");
        scallion.setDeleted(false);
        scallion.setCreatedAt(now.minusDays(4));
        scallion.setUpdatedAt(now.minusDays(1));
        saveOrUpdateMenuItem(scallion);
    }

    private void saveOrUpdateMenuItem(MenuItem item) {
        if (item.getRestaurateurId() == null) {
            return;
        }
        MenuItem existing = menuItemMapper.findByRestaurateurAndName(item.getRestaurateurId(), item.getName());
        if (existing == null) {
            if (item.getDeleted() == null) {
                item.setDeleted(Boolean.FALSE);
            }
            if (item.getCreatedAt() == null) {
                item.setCreatedAt(LocalDateTime.now());
            }
            if (item.getUpdatedAt() == null) {
                item.setUpdatedAt(item.getCreatedAt());
            }
            menuItemMapper.insert(item);
            log.info("Seeded menu item '{}' for restaurateur {}", item.getName(), item.getRestaurateurId());
            return;
        }
        item.setId(existing.getId());
        item.setDeleted(Boolean.FALSE);
        item.setCreatedAt(existing.getCreatedAt());
        if (item.getUpdatedAt() == null) {
            item.setUpdatedAt(existing.getUpdatedAt() != null ? existing.getUpdatedAt() : LocalDateTime.now());
        }
        menuItemMapper.update(item);
        log.info("Refreshed menu item '{}' for restaurateur {}", item.getName(), item.getRestaurateurId());
    }

    private void seedSampleOrders() {
        User guest = userMapper.getByUsername("guest");
        User restaurateurUser = userMapper.getByUsername("restaurateur");
        if (guest == null || restaurateurUser == null) {
            return;
        }
        Restaurateur restaurateur = restaurateurMapper.getByUserId(restaurateurUser.getId());
        if (guest == null || restaurateur == null) {
            return;
        }

        OrderInfo pending = new OrderInfo();
        pending.setOrderNo("OD" + System.currentTimeMillis());
        pending.setUserId(guest.getId());
        pending.setRestaurateurId(restaurateur.getId());
        pending.setRestaurantId(null);
        pending.setStatus("PENDING");
        pending.setRemark("少糖，提前10分钟送达");
        pending.setDeliveryAddress("上海市黄浦区外滩源路100号");
        pending.setCreatedAt(LocalDateTime.now().minusMinutes(25));
        pending.setUpdatedAt(pending.getCreatedAt());
        pending.setTotalAmount(new java.math.BigDecimal("102.00"));
        jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, deliveryman_id, restaurateur_id, restaurant_id, status, remark, delivery_address, total_amount, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                pending.getOrderNo(), pending.getUserId(), null, pending.getRestaurateurId(), pending.getRestaurantId(),
                pending.getStatus(), pending.getRemark(), pending.getDeliveryAddress(), pending.getTotalAmount(),
                pending.getCreatedAt(), pending.getUpdatedAt());
        Integer pendingId = jdbcTemplate.queryForObject("SELECT id FROM order_info WHERE order_no = ?", Integer.class, pending.getOrderNo());
        insertOrderItems(pendingId, new Object[][]{
                {"川味水煮鱼", "/uploads/dishes/spicy-fish.jpg", "68.00", 1},
                {"桂花酒酿圆子", "/uploads/dishes/osmanthus-riceball.jpg", "34.00", 1}
        });

        OrderInfo inProgress = new OrderInfo();
        inProgress.setOrderNo("OD" + (System.currentTimeMillis() - 100000));
        inProgress.setUserId(guest.getId());
        inProgress.setRestaurateurId(restaurateur.getId());
        inProgress.setStatus("IN_PROGRESS");
        inProgress.setRemark("忌辣，包装结实些");
        inProgress.setDeliveryAddress("上海市静安区南京西路1200号");
        inProgress.setCreatedAt(LocalDateTime.now().minusHours(1));
        inProgress.setUpdatedAt(LocalDateTime.now().minusMinutes(20));
        inProgress.setTotalAmount(new java.math.BigDecimal("56.00"));
        jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, deliveryman_id, restaurateur_id, restaurant_id, status, remark, delivery_address, total_amount, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                inProgress.getOrderNo(), inProgress.getUserId(), null, inProgress.getRestaurateurId(), null,
                inProgress.getStatus(), inProgress.getRemark(), inProgress.getDeliveryAddress(), inProgress.getTotalAmount(),
                inProgress.getCreatedAt(), inProgress.getUpdatedAt());
        Integer inProgressId = jdbcTemplate.queryForObject("SELECT id FROM order_info WHERE order_no = ?", Integer.class, inProgress.getOrderNo());
        insertOrderItems(inProgressId, new Object[][]{
                {"清新金枪鱼沙拉", "/uploads/dishes/tuna-salad.jpg", "36.00", 1},
                {"店制冰柠茶", null, "20.00", 1}
        });

        OrderInfo ready = new OrderInfo();
        ready.setOrderNo("OD" + (System.currentTimeMillis() - 250000));
        ready.setUserId(guest.getId());
        ready.setRestaurateurId(restaurateur.getId());
        ready.setStatus("READY");
        ready.setRemark("请送到前台");
        ready.setDeliveryAddress("上海市浦东新区世纪大道88号");
        ready.setCreatedAt(LocalDateTime.now().minusHours(3));
        ready.setUpdatedAt(LocalDateTime.now().minusMinutes(90));
        ready.setTotalAmount(new java.math.BigDecimal("88.00"));
        jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, deliveryman_id, restaurateur_id, restaurant_id, status, remark, delivery_address, total_amount, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                ready.getOrderNo(), ready.getUserId(), null, ready.getRestaurateurId(), null,
                ready.getStatus(), ready.getRemark(), ready.getDeliveryAddress(), ready.getTotalAmount(),
                ready.getCreatedAt(), ready.getUpdatedAt());
        Integer readyId = jdbcTemplate.queryForObject("SELECT id FROM order_info WHERE order_no = ?", Integer.class, ready.getOrderNo());
        insertOrderItems(readyId, new Object[][]{
                {"外滩牛排", "/uploads/dishes/steak.jpg", "88.00", 1}
        });

        // Additional orders for richer data
        OrderInfo processing1 = new OrderInfo();
        processing1.setOrderNo("OD" + (System.currentTimeMillis() - 350000));
        processing1.setUserId(guest.getId());
        processing1.setRestaurateurId(restaurateur.getId());
        processing1.setStatus("PROCESSING");
        processing1.setRemark("多加点辣椒");
        processing1.setDeliveryAddress("上海市徐汇区淮海中路999号");
        processing1.setCreatedAt(LocalDateTime.now().minusHours(2).minusMinutes(30));
        processing1.setUpdatedAt(LocalDateTime.now().minusMinutes(45));
        processing1.setTotalAmount(new java.math.BigDecimal("156.00"));
        jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, deliveryman_id, restaurateur_id, restaurant_id, status, remark, delivery_address, total_amount, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                processing1.getOrderNo(), processing1.getUserId(), null, processing1.getRestaurateurId(), null,
                processing1.getStatus(), processing1.getRemark(), processing1.getDeliveryAddress(), processing1.getTotalAmount(),
                processing1.getCreatedAt(), processing1.getUpdatedAt());
        Integer processing1Id = jdbcTemplate.queryForObject("SELECT id FROM order_info WHERE order_no = ?", Integer.class, processing1.getOrderNo());
        insertOrderItems(processing1Id, new Object[][]{
                {"玫瑰烤鸭", "/uploads/dishes/roast-duck.jpg", "128.00", 1},
                {"桂花酒酿圆子", "/uploads/dishes/osmanthus-riceball.jpg", "28.00", 1}
        });

        OrderInfo completed1 = new OrderInfo();
        completed1.setOrderNo("OD" + (System.currentTimeMillis() - 500000));
        completed1.setUserId(guest.getId());
        completed1.setRestaurateurId(restaurateur.getId());
        completed1.setStatus("COMPLETED");
        completed1.setRemark("");
        completed1.setDeliveryAddress("上海市长宁区愚园路288号");
        completed1.setCreatedAt(LocalDateTime.now().minusDays(1).minusHours(2));
        completed1.setUpdatedAt(LocalDateTime.now().minusDays(1));
        completed1.setTotalAmount(new java.math.BigDecimal("104.00"));
        jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, deliveryman_id, restaurateur_id, restaurant_id, status, remark, delivery_address, total_amount, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                completed1.getOrderNo(), completed1.getUserId(), null, completed1.getRestaurateurId(), null,
                completed1.getStatus(), completed1.getRemark(), completed1.getDeliveryAddress(), completed1.getTotalAmount(),
                completed1.getCreatedAt(), completed1.getUpdatedAt());
        Integer completed1Id = jdbcTemplate.queryForObject("SELECT id FROM order_info WHERE order_no = ?", Integer.class, completed1.getOrderNo());
        insertOrderItems(completed1Id, new Object[][]{
                {"川味水煮鱼", "/uploads/dishes/spicy-fish.jpg", "68.00", 1},
                {"清新金枪鱼沙拉", "/uploads/dishes/tuna-salad.jpg", "36.00", 1}
        });

        OrderInfo completed2 = new OrderInfo();
        completed2.setOrderNo("OD" + (System.currentTimeMillis() - 600000));
        completed2.setUserId(guest.getId());
        completed2.setRestaurateurId(restaurateur.getId());
        completed2.setStatus("COMPLETED");
        completed2.setRemark("不要香菜");
        completed2.setDeliveryAddress("上海市虹口区四川北路1688号");
        completed2.setCreatedAt(LocalDateTime.now().minusDays(2).minusHours(5));
        completed2.setUpdatedAt(LocalDateTime.now().minusDays(2).minusHours(3));
        completed2.setTotalAmount(new java.math.BigDecimal("236.00"));
        jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, deliveryman_id, restaurateur_id, restaurant_id, status, remark, delivery_address, total_amount, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                completed2.getOrderNo(), completed2.getUserId(), null, completed2.getRestaurateurId(), null,
                completed2.getStatus(), completed2.getRemark(), completed2.getDeliveryAddress(), completed2.getTotalAmount(),
                completed2.getCreatedAt(), completed2.getUpdatedAt());
        Integer completed2Id = jdbcTemplate.queryForObject("SELECT id FROM order_info WHERE order_no = ?", Integer.class, completed2.getOrderNo());
        insertOrderItems(completed2Id, new Object[][]{
                {"玫瑰烤鸭", "/uploads/dishes/roast-duck.jpg", "128.00", 1},
                {"外滩牛排", "/uploads/dishes/steak.jpg", "88.00", 1},
                {"店制冰柠茶", "/uploads/dishes/lemon-tea.jpg", "20.00", 1}
        });

        OrderInfo pending2 = new OrderInfo();
        pending2.setOrderNo("OD" + (System.currentTimeMillis() - 50000));
        pending2.setUserId(guest.getId());
        pending2.setRestaurateurId(restaurateur.getId());
        pending2.setStatus("PENDING");
        pending2.setRemark("尽快送达");
        pending2.setDeliveryAddress("上海市杨浦区国定路777号");
        pending2.setCreatedAt(LocalDateTime.now().minusMinutes(10));
        pending2.setUpdatedAt(pending2.getCreatedAt());
        pending2.setTotalAmount(new java.math.BigDecimal("176.00"));
        jdbcTemplate.update(
                "INSERT INTO order_info (order_no, user_id, deliveryman_id, restaurateur_id, restaurant_id, status, remark, delivery_address, total_amount, created_at, updated_at) " +
                        "VALUES (?,?,?,?,?,?,?,?,?,?,?)",
                pending2.getOrderNo(), pending2.getUserId(), null, pending2.getRestaurateurId(), null,
                pending2.getStatus(), pending2.getRemark(), pending2.getDeliveryAddress(), pending2.getTotalAmount(),
                pending2.getCreatedAt(), pending2.getUpdatedAt());
        Integer pending2Id = jdbcTemplate.queryForObject("SELECT id FROM order_info WHERE order_no = ?", Integer.class, pending2.getOrderNo());
        insertOrderItems(pending2Id, new Object[][]{
                {"玫瑰烤鸭", "/uploads/dishes/roast-duck.jpg", "128.00", 1},
                {"桂花酒酿圆子", "/uploads/dishes/osmanthus-riceball.jpg", "28.00", 1},
                {"店制冰柠茶", "/uploads/dishes/lemon-tea.jpg", "20.00", 1}
        });
    }

    private void seedChatData() {
        try {
            User restaurateurUser = userMapper.getByUsername("restaurateur");
            User guest = userMapper.getByUsername("guest");
            if (restaurateurUser == null || guest == null) {
                return;
            }
            Integer restaurateurId = restaurateurMapper.getByUserId(restaurateurUser.getId()).getId();

            // 选择最近的三张订单作为上下文，用于生成跨订单的聊天消息
            List<Integer> orderIds = jdbcTemplate.query(
                    "SELECT id FROM order_info WHERE restaurateur_id = ? ORDER BY created_at DESC LIMIT 3",
                    (rs, rowNum) -> rs.getInt(1), restaurateurId);
            Integer orderId = orderIds.isEmpty() ? null : orderIds.get(0);

            String sessionKey = "FREE:GUEST:" + guest.getId();
            ChatSession exist = chatSessionMapper.findByKey(sessionKey);
            if (exist == null) {
                ChatSession s = new ChatSession();
                s.setSessionKey(sessionKey);
                s.setRestaurateurId(restaurateurId);
                s.setPeerUserId(guest.getId());
                s.setPeerRole("GUEST");
                s.setOrderId(orderId);
                s.setContextType(orderId == null ? null : "ORDER");
                s.setContextRef(orderId);
                s.setTitle("体验用户 · 订单历史");
                s.setCreatedAt(LocalDateTime.now().minusHours(5));
                s.setUpdatedAt(s.getCreatedAt());
                chatSessionMapper.insert(s);
                exist = chatSessionMapper.findByKey(sessionKey);
            }
                if (exist != null) {
                // 插入跨订单的历史消息（>30条），并混合已读/未读标志
                LocalDateTime base = LocalDateTime.now().minusHours(4);
                List<String> head = Arrays.asList(
                        "你好，我想咨询一下配送时间大概多久？",
                        "您好，当前大概需要30分钟左右哦~",
                        "可以少辣吗？家里有小孩。",
                        "没问题，已备注少辣。"
                );
                for (int i = 0; i < head.size(); i++) {
                    boolean byGuest = (i % 2 == 0);
                    Integer sender = byGuest ? guest.getId() : restaurateurUser.getId();
                    String sRole = byGuest ? "GUEST" : "RESTAURATEUR";
                    Integer receiver = byGuest ? restaurateurUser.getId() : guest.getId();
                    String rRole = byGuest ? "RESTAURATEUR" : "GUEST";
                    Integer oid = orderIds.isEmpty() ? orderId : orderIds.get(0);
                    insertChat(exist.getId(), oid, sender, sRole, receiver, rRole, head.get(i), base.plusMinutes(3*i));
                }

                String[] extras = {
                        "好的，感谢~", "请问可以加一双筷子吗？", "当然可以，已为您添加。", "配送员正在赶来。",
                        "辛苦啦！", "收到~", "还有优惠活动吗？", "关注店铺可享更多优惠哦",
                        "今天订单较多，稍有延迟，抱歉啦", "没关系，慢慢来", "你们家的酸辣粉真的好吃！", "谢谢夸奖～",
                        "下次还会再来", "欢迎常来", "再见～", "祝您生活愉快",
                        "今晚有新品上架~", "期待！", "优惠券怎么用？", "下单页勾选即可"
                };
                for (int i = 0; i < extras.length; i++) {
                    boolean byGuest = (i % 2 == 0);
                    Integer sender = byGuest ? guest.getId() : restaurateurUser.getId();
                    String sRole = byGuest ? "GUEST" : "RESTAURATEUR";
                    Integer receiver = byGuest ? restaurateurUser.getId() : guest.getId();
                    String rRole = byGuest ? "RESTAURATEUR" : "GUEST";
                    // 轮流分配到最近的三张订单
                    Integer oid = orderIds.isEmpty() ? orderId : orderIds.get(i % Math.max(orderIds.size(),1));
                    LocalDateTime ct = base.plusMinutes(12 + i);
                    insertChat(exist.getId(), oid, sender, sRole, receiver, rRole, extras[i], ct);
                    // 将较早的消息标记为已读（模拟对端已查看）
                    if (i < 8) {
                        jdbcTemplate.update("UPDATE chat_message SET read_flag=1 WHERE session_id=? ORDER BY id ASC LIMIT ?",
                                exist.getId(), i/2 + 1);
                    }
                }

                // 更新会话最后消息
                LocalDateTime lastTime = base.plusMinutes(12 + extras.length);
                jdbcTemplate.update("UPDATE chat_session SET last_message_preview=?, last_message_time=?, updated_at=? WHERE id=?",
                        "下单页勾选即可", java.sql.Timestamp.valueOf(lastTime), java.sql.Timestamp.valueOf(lastTime), exist.getId());
                }
        } catch (Exception ex) {
            log.warn("Seed chat data skipped: {}", ex.getMessage());
        }
    }

    private void insertChat(Integer sessionId, Integer orderId,
                             Integer senderId, String senderRole,
                             Integer receiverId, String receiverRole,
                             String content, LocalDateTime time) {
        if (sessionId == null) return;
        ChatMessage m = new ChatMessage();
        m.setSessionId(sessionId);
        m.setOrderId(orderId);
        m.setContextType(orderId == null ? null : "ORDER");
        m.setContextRef(orderId);
        m.setSenderId(senderId);
        m.setSenderRole(senderRole);
        m.setReceiverId(receiverId);
        m.setReceiverRole(receiverRole);
        m.setContent(content);
        m.setReadFlag(Boolean.FALSE);
        m.setCreatedAt(time);
        chatMessageMapper.insert(m);
    }

    private void insertOrderItems(Integer orderId, Object[][] items) {
        if (orderId == null) {
            return;
        }
        for (Object[] item : items) {
            jdbcTemplate.update(
                "INSERT INTO order_item (order_id, dish_name, dish_image, unit_price, quantity) VALUES (?,?,?,?,?)",
                orderId,
                item[0],
                item[1],
                new java.math.BigDecimal(item[2].toString()),
                item[3]
            );
        }
    }

    private void ensureColumnWithCheck(String table, String column, String ddl) {
        if (!tableExists(table)) {
            return;
        }
        if (!columnExists(table, column)) {
            try {
                jdbcTemplate.execute(ddl);
                log.info("Added {}.{} column", table, column);
            } catch (DataAccessException ex) {
                log.warn("Failed to add {}.{} column: {}", table, column, ex.getMessage());
            }
        }
    }

    private boolean columnExists(String tableName, String columnName) {
        String sql = "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private boolean tableExists(String tableName) {
        String sql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName);
        return count != null && count > 0;
    }

}
