# Play System TODO 清单

## 短期优先
- **完善鉴权链路**  
  - 实现 `system-server/src/main/java/com/example/security/JwtAuthInterceptor.java` 与 `JwtUtil.java`。  
  - 前端在 `system-ui/src/stores/auth.ts` 与 API 调用中持久化并透传 Token。
- **替换占位页面**  
  - 以真实功能实现 `system-ui/src/views/placeholder/RestaurateurDishes.vue`、`RestaurateurOrders.vue` 等关键商家页面。  
  - 结合后端 `MenuItemServiceImpl` 和 `OrderServiceImpl` 提供完整流程。
- **聊天稳健性增强**  
  - 在 `ChatWorkspace.vue` 中补充断线重连、错误提示与发送失败重试。  
  - 后端 `ChatController` 和 `ChatWebSocketController` 加入失败回执与日志监控。
- **安全配置**  
  - 将 `application.yml` 中的 DeepSeek 默认密钥替换为环境变量或密钥管理方案。  
  - 对文件上传目录 `uploads/` 增加访问控制与清理策略。

## 中期规划
- **监控与分析**：为商家/平台提供订单、营收与配送效率的可视化面板。  
- **通知中心**：整合邮件、短信或站内通知，提醒订单状态变化与聊天未读。  
- **端到端测试**：利用根目录 Playwright 依赖建立 UI 与 API 自动化回归套件。  
- **移动端体验**：迭代移动端布局与 PWA 支持，提升跨设备体验。

## 长期展望
- **开放平台**：为第三方餐饮系统提供 API/SDK 接入，拓展生态。  
- **智能调度**：结合地图数据与历史配送表现，实现订单智能派单与路径优化。  
- **多语言与多地区**：支持多语言界面、货币与税率配置，扩展国际化部署。

