---
agent: agent
---

# Play System 需求提示词文档（后端 Spring Boot + 前端 Vue3）

本提示词基于当前仓库代码扫描（后端 system-server，前端 system-ui）与原始需求合并完善，旨在为后续工程迭代与 AI 生成提供统一、可执行的规范与约束。

## 项目背景（Project Background）
- 定位：本项目是一个外卖/点餐平台的多角色系统，包含用户（顾客）、商家、骑手、管理员四类主体，覆盖点餐、聊天、订单履约、评价与经营分析。
- 目标用户：
	- 顾客：浏览餐馆与菜单、下单、评价、与商家/骑手沟通。
	- 商家：维护餐馆资料与菜品、处理订单、与顾客/骑手沟通、查看经营数据。
	- 骑手：接单配送、状态流转、与商家/顾客沟通。
	- 管理员：用户与数据治理、系统监控与统计。
- 技术栈与约束：
	- 后端：Spring Boot 3.5.x（Java 17）、MySQL、JPA + MyBatis（Mapper XML）、内置 STOMP WebSocket（SockJS 可回退）。
	- 前端：Vue 3 + Vite + TypeScript + Pinia + Vue Router，Chart.js 数据可视化，STOMP/SockJS 客户端。
	- 地图：高德地图 Web JS API（含安全码 jscode 注入的本地代理与 vite 代理）。
	- 构建与运行：后端默认 8080 端口；前端 vite dev，/api 与 /ws 走本地代理到 8080。
- 商业目标：提升履约与客服效率，沉淀经营数据；聊天与订单强关联，减少纠纷；提供轻量可扩展的后台管理能力。 
- 核心非功能约束：
	- 界面需美观、响应式、可访问；移动端/小屏体验良好。
	- 聊天与订单强关联；消息可靠送达、断线可重连、未读提醒与历史可回溯。
	- 所有接口响应统一包裹；分页、筛选、排序规则统一；具备基础幂等与错误码约定。

## 项目需求（Project Requirements）

### 功能相关

#### 1）商家相关（餐馆端功能）
1. 餐馆信息管理
	 - 行为说明：
		 - 在“餐馆信息”界面维护地图定位、餐馆名称（必填，可修改）、餐馆介绍（可选）、餐馆图片；可查看收到的评价列表，支持点赞与展开详情。
		 - 地图使用高德 API，需能聚焦到具体位置，允许通过标注拖拽/点击修正坐标；坐标与名称、地址不可为空（坐标必填）。
	 - 输入/输出：
		 - 前端组件：`RestaurantRegisterPanel.vue`（已集成 AMap 与上传预览）；后端接口：
			 - GET `/api/restaurant/list` → 展示地图上的餐馆（简要信息）。
			 - POST `/api/restaurant/profile/get` → 获取当前商家餐馆资料。
			 - POST `/api/restaurant/profile/save` → 保存餐馆资料（name、address、lng、lat、photoUrl、description、restaurateurId）。
			 - POST `/api/restaurant/uploadPhoto`（multipart）→ 返回 `{ status, message, data:{ url } }`。
		 - 数据约束：`name`、`address` 必填，`lng/lat` 必填且为合法经纬度。
	 - 成功/失败条件：保存后可重新获取并一致展示；地图定位与坐标持久化；上传失败需提示。
	 - 边界/异常：图片过大需压缩（前端已做）、地理编码失败需提示重试。

2. 运营概况（可视化）
	 - 行为说明：
		 - 展示订单数量、收入、菜品销量等关键指标，支持按时间维度选择（日/周/月/自定义范围）；统一用 Chart.js 可视化。
		 - 去除“我的餐厅”无关模块，仅保留数据图表与筛选控件。
	 - 输入/输出：
		 - 后端（待完善真实统计）现有：GET `/api/restaurateur/stats`（当前为 mock）。
		 - 建议新增：
			 - GET `/api/restaurateur/metrics?from=...&to=...&granularity=day|week|month` → 返回各指标时序数据。
			 - GET `/api/restaurateur/menu/tops?from=...&to=...&limit=10` → 返回畅销菜品榜。
	 - 成功/失败条件：切换时间窗时图表与指标同步刷新；空数据也应渲染占位与空态。
	 - 边界/异常：跨月/跨年统计；时区与当日零点边界；数据量大时分页/取样。

3. 菜品管理（卡片化 + 分页 + 软删除）
	 - 行为说明：
		 - 列表以“卡片”展示，上架绿色、下架黄色；卡片展示名称、价格、类别、图片与“详情”标签；点击图片查看详情（图片、名称、价格、介绍、类别、状态）。
		 - 查询支持关键词与分类过滤；结果分页展示。支持增删改查，删除为软删除，主键使用自增 `id`。
	 - 输入/输出：
		 - 现有接口：
			 - GET `/api/restaurateur/menu?restaurateurId&keyword&category&status&page&size`
			 - GET `/api/restaurateur/menu/{id}?restaurateurId=...`
			 - GET `/api/restaurateur/menu/categories?restaurateurId=...`
			 - POST `/api/restaurateur/menu?restaurateurId=...`（JSON）
			 - PUT `/api/restaurateur/menu/{id}?restaurateurId=...`（JSON）
			 - DELETE `/api/restaurateur/menu/{id}?restaurateurId=...`
		 - 返回统一 `{ status, message, data }`；分页 data 为 `{ total, records: MenuItemDto[] }`。
	 - 成功/失败条件：创建/更新后列表刷新，状态色块正确；软删除后不影响历史订单。
	 - 边界/异常：重名校验、价格边界、类别空/其他；并发更新的幂等与乐观锁建议。

4. 聊天（与订单强关联,用websocket实现相关细节）
	 - 行为说明：
		 - 左侧 1/5 宽度显示会话列表（顾客/骑手两类用不同色标识），列表项包含对象名称、类型、最近一条消息时间与内容（>15 字省略）与未读角标；新会话置顶。
		 - 右侧 4/5 为聊天区，顶部展示“订单详情（号、状态、备注、创建时间）”，中部为历史消息（可向上加载更多），底部输入框（Ctrl+Enter 发送）。
		 - 通过“联系商家/联系骑手”从订单进入，自动创建/聚合会话；会话按“订单 + 对端角色 + 对端ID”唯一。
	 - 输入/输出：
		 - HTTP：
			 - POST `/api/chat/sessions`（{ username }）→ `ChatSessionSummary[]`（含 unreadCount）。
			 - POST `/api/chat/history`（{ orderId, peerId, peerRole, beforeId?, size?, username? }）→ `{ messages: ChatMessageDto[], order: ChatOrderSnippet }`。
			 - POST `/api/chat/markRead`（同 history 的定位参数）→ 标记已读。
			 - POST `/api/chat/send`（{ orderId, receiverId, receiverRole, content, username }）→ 新消息。
		 - WS（STOMP）：
			 - 连接端点：`/ws/chat`（SockJS 可回退）；`CONNECT` 头部需包含 `username`。
			 - 应用前缀：`/app`；发送目的地：`/app/chat/send`。
			 - 个人队列订阅路径：`/user/queue/chat`。
			 - 服务端事件：
				 - 正常投递：`{ message: ChatMessageDto, session: ChatSessionSummary }`
				 - 错误：`{ type: 'ERROR', message: string }`
				 - 会话刷新提示（可选）：`{ type: 'SESSION_REFRESH' }`
	 - 成功/失败条件：
		 - 连接成功后订阅收到自身相关消息；发送后本地回显，后端确认到达（HTTP 返回或 WS 回执）。
		 - 活动会话消息到达后自动标记已读；非活动会话未读数 +1。
	 - 边界/异常：离线期间消息存储；重复连接与断线重连（指数回退 + 心跳）；大消息拆分/限制；非法 orderId/权限拒绝。

5. 接收订单（商家处理流）
	 - 行为说明：
		 - 订单状态：`未接单` → `处理中` → `已出餐` → `已完成`（已完成由骑手确认）；商家界面默认优先展示“未接单”。
		 - 列表以小卡片展示：下单人、时间、金额、送达地点，层次分明；点击进入详情可见菜品项、备注、金额等。
		 - 状态动作：商家可将订单流转到下一步（幂等，防重复点击）。
		 - 完成结转：订单达“已完成”时，金额汇总到商家收入（影响运营概览）。
	 - 建议接口：
		 - GET `/api/order/pending?restaurateurId&page&size`
		 - GET `/api/order/{id}`
		 - POST `/api/order/{id}/accept`
		 - POST `/api/order/{id}/start-cooking`
		 - POST `/api/order/{id}/ready`
		 - POST `/api/order/{id}/complete`（骑手端）
	 - 成功/失败条件：状态不可逆/不可跳跃；流转成功后刷新列表与统计；并发更改需校验版本。
	 - 边界/异常：超时未接单自动取消或提醒；骑手异常（重新指派）；取消/退款流程对账。

#### 2）用户相关（客户端功能）
- 账号：注册（角色=GUEST）、登录、个人资料与头像上传（`/api/user/uploadAvatar`）。
- 浏览：地图找店（`/api/restaurant/list`）、餐馆详情（含评价）、菜单与购物车、下单、支付（支付集成可后续接入）。
- 评价：对餐馆评价与点赞（`/api/restaurant/reviews/create`、`/api/restaurant/reviews/like`）。
- 订单：查看订单列表与详情、状态流转提醒、从订单页“联系商家/骑手”进入聊天室。
- 聊天：与商家/骑手双向沟通；从订单页进入会话；WS 与 HTTP 接口与商家端一致。
- 统计：`/api/user/stats`（当前为 mock），建议扩展订单数、消费总额、常点餐馆等。

#### 3）骑手相关（配送端功能）
- 账号：注册（角色=DELIVERYMAN）、登录、资料维护。
- 接单/配送：待取餐、配送中、已完成；地图查看取餐/送达点；状态更新影响商家与顾客的订单状态。
- 聊天：与商家/顾客双向沟通；从订单页进入会话；WS 与 HTTP 接口与商家端一致。
- 统计：`/api/deliveryman/stats`（当前为 mock），建议扩展配送效率、里程、评分等。

#### 4）管理员相关（后台管理）
- 用户治理：用户列表、角色管理，接口：`/api/user/getAllUsers`、`/api/user/getByUsername`。
- 数据面板：用户数、订单量、GMV、餐馆数、菜品数等；建议 `/api/admin/metrics` 聚合。
- 审计与风控（预留）：登录日志、异常高频下单、评价敏感词等。

### 数据相关
- 重要表/实体（MySQL；实体与 MyBatis Mapper 已存在，以下字段为建议/对齐）：
	- user（用户）
		- id PK, username UNIQUE, password(hash), role ENUM('GUEST','RESTAURATEUR','DELIVERYMAN','ADMIN'), nickname, email, phone, avatar_url, created_at, updated_at
		- 索引：username UNIQUE；role；created_at
	- restaurant（餐馆）
		- id PK, restaurateur_id FK(user.id), name, address, lng DECIMAL(10,6), lat DECIMAL(10,6), description TEXT, image_url, created_at, updated_at
		- 索引：restaurateur_id；(lng, lat)；name
	- menu_item（菜品）
		- id PK, restaurateur_id FK, name, price DECIMAL(10,2), category, image_url, description, status ENUM('ON_SHELF','OFF_SHELF'), is_deleted TINYINT(1) DEFAULT 0, created_at, updated_at
		- 索引：restaurateur_id；(restaurateur_id, category)；status
	- order（订单）
		- id PK, order_no, user_id, restaurateur_id, deliveryman_id NULL, status ENUM('PENDING','PROCESSING','READY','COMPLETED','CANCELLED'), amount DECIMAL(10,2), remark, address, created_at, updated_at, completed_at
		- 索引：order_no UNIQUE；(restaurateur_id, status)；(user_id, created_at)
	- order_item（订单项）
		- id PK, order_id FK, menu_item_id FK, name_snapshot, price_snapshot, quantity
		- 索引：order_id
	- chat_message（聊天消息）
		- id PK, session_id VARCHAR, order_id FK, sender_id, sender_role, receiver_id, receiver_role, content, read TINYINT(1), created_at
		- 索引：(session_id, id)；(receiver_id, receiver_role, read)
	- chat_session（会话聚合，可选持久化）
		- session_id PK, order_id, peer_id, peer_role, peer_name, last_message, last_message_time, unread_count
		- 可通过消息派生更新。
	- restaurant_review（评价）/ restaurant_review_like（点赞）与 xml 中实体对应。
- 事务边界：
	- 订单状态流转应在单事务内完成并校验版本（optimistic locking 或 where status = prevStatus）；完成时记账（收入统计表或按需计算）。
	- 聊天消息写入与未读计数更新同事务。
- 数据校验：
	- 价格范围与格式；经纬度合法；角色权限校验（订单范围、聊天对象）。

### WebSocket 聊天设计
- 握手鉴权：
	- 客户端通过 STOMP CONNECT 头部传入 `username`；服务端 `WebSocketAuthChannelInterceptor` 将其注入为 Principal。
	- 建议后续接入 JWT：在 CONNECT 头部传 `Authorization: Bearer <token>` 并在拦截器内校验，解析后设置 Principal。
- 连接端点与目的地：
	- 端点：`/ws/chat`（SockJS 备用），应用前缀 `/app`，用户目的地 `/user/queue`。
	- 发送：`/app/chat/send`；订阅：`/user/queue/chat`。
- 消息事件类型与结构：
	- 正常投递：
		{
			"message": {
				"id": 123,
				"sessionId": "<orderId:peerRole:peerId>",
				"orderId": 456,
				"senderId": 11,
				"senderRole": "RESTAURATEUR",
				"receiverId": 99,
				"receiverRole": "GUEST",
				"content": "已开始制作",
				"read": false,
				"createdAt": "2025-10-20T12:00:00"
			},
			"session": {
				"sessionId": "456:GUEST:99",
				"orderId": 456,
				"peerId": 99,
				"peerRole": "GUEST",
				"peerName": "张三",
				"lastMessage": "已开始制作",
				"lastMessageTime": "2025-10-20T12:00:00",
				"unreadCount": 1
			}
		}
	- 错误：{ "type": "ERROR", "message": "发送失败" }
	- 刷新提示（可选）：{ "type": "SESSION_REFRESH" }
- Ack/回执：
	- 发送侧本地先行回显一条临时消息（负 id），收到服务端确认（HTTP 返回或 WS 推送）后以服务端消息替换；失败则提示重试。
- 断线重连：
	- 客户端使用 `reconnectDelay` 与心跳；断开后自动重连，成功后重新订阅并全量刷新会话列表。
- 离线消息：
	- 未读消息按接收方 user+role 聚合；客户端登录或重连后先拉取 `/api/chat/sessions` 与 `/api/chat/history`；服务端按 `read=false` 返回并在前端标已读时更新。

### 界面设计（UI/UX）
- 页面清单（现有 + 建议）：
	- 顾客端：
		- 首页/地图找店（`views/GDMap.vue`、`views/GuestHome.vue`）：地图聚合标注、点击进入餐馆详情。
		- 餐馆详情：菜单、评价、点赞、下单。
		- 订单列表/详情：状态流转提示、联系商家/骑手入口。
		- 聊天（`views/ChatWorkspace.vue`）：与商家/骑手双向沟通。
	- 商家端：
		- 登录（`views/LoginView.vue`）、主页（`views/RestaurateurHome.vue`）。
		- 餐馆信息：地图 + 基本资料（`components/RestaurantRegisterPanel.vue`）。
		- 菜品管理：卡片化列表、分页、筛选、详情抽屉、增删改查。
		- 接收订单：待接单优先，卡片化展示，流转操作。
		- 聊天工作台（`ChatWorkspace.vue`）：按订单聚合。
		- 运营概况：Chart.js 多图表时间轴筛选。
	- 骑手端：
		- 首页：待取餐、配送中、已完成；地图导航；订单详情；聊天入口。
	- 管理端：
		- 用户管理、指标面板（可基于现有组件扩展）。
- 关键交互：
	- 会话列表未读红点；滚动加载历史；Ctrl+Enter 发送；图片点击预览。
	- 地图选择位置后自动逆地理编码补全地址；标注可拖拽修正坐标并缩放聚焦。
- 响应式与可访问性：
	- 小屏隐藏右侧详情栏，优先展示会话与消息；语义化标签、可聚焦控件、对比度达标。

## 前后端设计规范
- API 约定：
	- 统一响应：`{ status: number, message?: string|null, data?: any }`（已在后端 `Result` 实现）。
	- 分页请求：POST 场景用 `{ data: {...}, page?: number, size?: number }`；GET 场景用查询参数 `page/size`；分页响应 data 统一 `{ total: number, records: any[] }`。
	- 错误码区间：
		- 200：成功；400~499：客户端错误（参数、权限）；500~599：服务端错误。
		- 目前后端 `Result.error` 返回 500，建议引入更细化码：如 401/403/404/409/422。
- 鉴权：
	- 现状：未启用 Spring Security；多数接口从请求体 `username` 或本地存储推断用户。
	- 建议：分阶段接入 JWT；HTTP 通过 `Authorization: Bearer`，WS 通过 CONNECT 头或 query param 传递；服务端统一解析成 Principal。
- 接口幂等性：
	- 订单流转接口需幂等（基于版本或状态前置校验）。
	- 上传/创建接口可使用业务幂等键（如 `clientRequestId`）。
- 版本管理：
	- 预留 `/api/v1/...` 前缀的能力；当前先维持 `/api/...`。
- 分页与筛选约定：
	- GET 列表统一支持 `keyword`、`category`、`status` 等常见过滤；返回 `total`、`records`。

## 代码规范
- 后端（Java）
	- 风格：Google Style / Spring 推荐，Lombok 谨慎使用（DTO/VO 可用，Entity 慎用 @Data）。
	- 包结构：`controller`/`service`/`entity`/`mapper`/`dto`/`common`/`config`/`util`。
	- 日志：使用 `slf4j`；入参/关键出参记录 debug；异常记录 error。
	- 异常：业务异常转为标准 `Result`；参数校验使用 `spring-boot-starter-validation`。
	- 测试：`@SpringBootTest` + 切片测试（mapper/service）；关键路径（订单流转、聊天投递）需覆盖。
- 前端（Vue 3 + TS）
	- 组件：`<script setup lang="ts">`，组合式 API；状态集中到 Pinia；API 模块化（见 `src/api/*`）。
	- 样式：模块化/Scoped；设计系统化（色板、间距、层级）；移动优先。
	- 代码质量：ESLint + vue-tsc；提交前 `lint` 与 `type-check`。
- 提交规范
	- Commit Message：`type(scope): subject`（feat, fix, refactor, docs, chore, test, style）。
	- 分支：`feat/*`、`fix/*`、`chore/*`；PR 模板包含变更说明与验证记录。
- CI 建议
	- 步骤：install → lint → type-check → build（前端）/ test（后端） → 打包。

## 验收标准与交付物（Acceptance Criteria）
- 功能实现：
	- 商家五大功能（餐馆信息/运营概况/菜品管理/聊天/接单）按上文行为与约束完整实现，无重大逻辑错误。
	- 用户、骑手、管理员基础功能可用；聊天收发、未读、历史、断线重连有效。
- 接口文档：
	- 至少通过 Swagger/OpenAPI 或 Postman 集合提供可运行示例。
	- 鉴权、错误码、分页与筛选示例齐全。
- 测试：
	- 后端：关键服务单测通过；基础集成测试运行无错误。
	- 前端：关键页面最小化 E2E（登录、聊天收发、菜单 CRUD）。
- 其他交付物：
	- `.env.example`（前端）含 `VITE_WS_CHAT_ENDPOINT`、`VITE_WS_CHAT_SEND_DEST`、`VITE_WS_CHAT_SUB`、`VITE_AMAP_SECURITY_JSCODE` 等占位；README 启动说明与代理说明（`proxy-server.js`）。

## 示例任务（上手指南）
1. 实现订单接单流：新增 `/api/order/*` 控制器与服务，打通状态机与统计联动，并在商家端新增“接收订单”视图与卡片列表。
2. 完善运营概况：实现 `/api/restaurateur/metrics` 与前端图表时间轴筛选，支持日/周/月与自定义时间窗。
3. 聊天消息可靠性：为 WS 发送增加“本地临时消息 + 确认替换”逻辑，服务端为 `/app/chat/send` 返回 ack 类型事件，前端接收后替换负 id 记录。

## 变更记录模板（建议）
- 文件：`prompt.txt`
- 流程：
	- 任何需求或约定的调整均在 PR 中同时更新 `prompt.txt`，并在本节新增一条，包含：日期、变更项、影响范围、迁移指南。
- 模板示例：
	- 2025-10-22 feat: 新增订单接单接口规范；影响：商家端与骑手端；迁移：前端新增状态流转按钮，后端添加状态校验。

---

# 附：现有接口与前端约定要点（速查）
- 统一响应：`{ status, message, data }`
- 重要现有接口：
	- 用户：`/api/user/login`、`/api/user/register`、`/api/user/getByUsername`、`/api/user/uploadAvatar`、`/api/user/stats`。
	- 餐馆：`/api/restaurant/list`、`/api/restaurant/profile/get|save`、`/api/restaurant/uploadPhoto`、`/api/restaurant/reviews/page|like|create`。
	- 菜单：`/api/restaurateur/menu`（GET 分页、POST 创建、PUT 更新、DELETE 软删、GET /categories、GET /{id}）。
	- 聊天 HTTP：`/api/chat/sessions|history|markRead|send`。
	- 聊天 WS：端点 `/ws/chat`，发送 `/app/chat/send`，订阅 `/user/queue/chat`。
- 代理：Vite dev server 代理 `/api` 与 `/ws`→ `http://localhost:8080`；AMap 代理见 `vite.config.ts` 与 `proxy-server.js`。

## 变更说明（对原始 prompt.txt 的补充/修改点）
- 保留并扩展了“商家相关”五大功能要求，细化为“行为说明 + 输入/输出 + 成功/失败条件 + 边界/异常”四段结构。
- 新增“项目背景”“用户/骑手/管理员”三类主体需求，完善全角色视角。
- 基于现有代码补充了接口清单与路径（`/api/user/*`, `/api/restaurant/*`, `/api/restaurateur/menu`, `/api/chat/*`），统一响应包结构与分页约定。
- 明确 WebSocket 设计（端点、目的地、消息结构、ack/回执、重连与离线消息处理），与前端 `ChatWorkspace.vue`、后端 `ChatWebSocketController`/`WebSocketConfig` 对齐。
- 提供数据模型建议（user/restaurant/menu_item/order/chat_message 等）与索引/事务边界，兼容现有 MyBatis XML 结构。
- 补充前后端代码规范、提交与 CI 建议，增强工程可执行性。
- 新增验收标准与交付物、3 个短期上手任务、以及 prompt.txt 变更记录模板。
