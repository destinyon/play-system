# 阶段 2 · 前端契约同步

## 本阶段交付
- 在 `system-ui/src/views/styles/` 中为核心视图抽离样式文件：LoginView、ChatWorkspace、AIChatView、GuestHome、DeliverymanHome、RestaurateurHome、AboutView、HomeView、GDMap、MarkersView、RoutesView 均改用 `<style scoped src="./styles/xxx.css">` 引入，后续占位页可按需迁移，抽离后的 CSS 便于复用与主题化。
- 前端鉴权链路与 HTTP 封装对齐后端协议：`auth` store 持久化 token/role，`postDataRequest` 默认补全 Authorization 头，请求/响应统一复用 `buildAuthHeaders`。
- 商家核心页面脱离占位实现：`restaurateur/DashboardView`、`OrdersView`、`DishesView`、`RestaurantProfileView` 迁移至正式目录并共享最新 API 契约。
- 聊天工作区体验强化：新增断线提示、自动重连、失败回执与“重试发送”，WebSocket/STOMP 统一携带 token 头。

## 下一步
- 实现统一的表单校验反馈组件（Toast + 文案提示），同时继续推进剩余页面与组件的样式梳理和变量化。
