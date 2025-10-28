# Play System 项目概览

## 项目背景
- Play System 是一体化的餐饮服务平台，定位于“连接食客、商家与配送员”的全流程运营系统。
- 项目支持外卖点餐、配送调度、评价反馈与 AI 助手等场景，目标是提升本地餐饮数字化运营效率。

## 系统结构
- **system-ui**：基于 Vue 3 + TypeScript + Vite 的前端，负责多角色界面与实时交互体验。
- **system-server**：基于 Spring Boot 3 的后端，整合 REST、WebSocket、MyBatis 与 JPA 等组件。
- **共享设施**：AI 上下文、静态资源、上传目录与 Playwright 自动化依赖放置在仓库根目录下。

## 核心功能模块
- **用户与账号**：注册登录、角色识别、个人资料（`system-ui/src/stores/auth.ts`）。
- **餐馆管理**：商家入驻、餐馆信息维护、地图定位与图片上传（`system-server/.../RestaurantServiceImpl.java`）。
- **菜品与菜单**：菜品 CRUD、分类、库存状态与价格展示（MyBatis XML + Service）。
- **订单履约**：订单创建、状态流转、历史查询与统计（`OrderService*` 系列）。
- **配送调度**：配送员接单、路线展示、进度更新（地图视图 `GDMap.vue`、`RoutesView.vue`）。
- **即时通信**：三方聊天工作区、未读通知、WebSocket 推送（`ChatWorkspace.vue` + STOMP）。
- **AI 助手**：DeepSeek 问答、项目上下文注入、上传附件解析（`AIChatView.vue` + `AIController.java`）。

## 角色定位
- **Guest（食客）**：浏览餐馆、下单、追踪配送、评价口碑。
- **Restaurateur（商家）**：管理餐馆与菜品、处理订单、回复客户。
- **Deliveryman（配送员）**：接单配送、查看路线、与客商沟通。
- **Admin（管理员）**：预留仪表盘与全局配置入口，支持后续扩展。

## 技术栈
- **前端**：Vue 3、TypeScript、Pinia、Vue Router、STOMP/SockJS、Vite、ESLint/Vue TSC。
- **后端**：Spring Boot 3、Spring WebSocket、Spring Data JPA、MyBatis、Jakarta Validation。
- **数据库**：MySQL（`takeout_system`），通过 JPA + MyBatis 混合访问。
- **自动化与工具**：Playwright、npm-run-all、Lombok、DeepSeek API 代理。

## 集成要点
- 统一的 `DataRequest`/`Result` 协议让前后端对齐分页信息与状态码。
- WebSocket 采用 `/ws/chat` + `/app` 路径，结合 `WebSocketAuthChannelInterceptor` 完成身份注入。
- AI 上下文可由前端动态注入，后端读取 `resources/ai/context.md` 以增强回答准确性。

