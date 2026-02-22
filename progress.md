# PRD 对应进度清单

基准文档：
- `PRD_企业级私教工作室管理系统_发布版_v1.1.md`（位于仓库上级目录）
- `MVP_页面接口数据表清单.md`（位于仓库上级目录）

更新时间：2026-02-22

## 2.3 MVP 范围（PRD）
- [x] 会员&线索 CRM 基础（线索/会员闭环已打通）
- [x] 私教排班/预约/取消（核心接口已完成）
- [x] 签到与课消（含幂等与冲正）
- [x] 收银与退款（支付宝沙箱模拟流程已打通）
- [x] 教练提成 + 基础经营报表

## 3 用户与权限（PRD）
- [x] RBAC 基础接口（角色查询、用户角色分配）
- [x] 租户/门店上下文拦截（`tenant/store`）
- [x] 关键操作审计日志查询接口
- [ ] 页面按钮级权限控制（前端未开始）

## 4 业务功能（PRD）
### 4.2 CRM
- [x] 线索录入、编辑、详情、列表
- [x] 跟进记录
- [x] 线索转会员
- [x] 会员列表/详情/编辑/时间线

### 4.3 排班与预约
- [x] 教练档案管理
- [x] 时段配置
- [x] 可约时段查询
- [x] 预约创建/取消与冲突校验

### 4.4 签到与课消
- [x] 签到接口
- [x] 课消接口（幂等键）
- [x] 课消冲正
- [ ] 补签审批流（未开始）

### 4.5 收银
- [x] 订单创建/查询
- [x] 支付预下单
- [x] 支付回调处理（含幂等）

### 4.6 退款与对账
- [x] 退款申请
- [x] 退款审批通过/拒绝
- [x] 日结对账接口

### 4.7 提成与报表
- [x] 提成规则配置（版本化）
- [x] 提成单生成
- [x] 提成单锁账
- [x] 三类基础报表（总览/到课/财务）

### 4.8 门店管理
- [x] 门店配置（`/api/settings/store`）
- [ ] 角色权限配置页面（后端基础已具备）
- [x] 审计日志查询

## F. MVP 接口清单映射
### F.1 认证与权限
- [x] `POST /api/auth/login`
- [x] `POST /api/auth/logout`
- [x] `GET /api/auth/me`
- [x] `GET /api/rbac/roles`
- [x] `POST /api/rbac/users/{id}/roles`

### F.2 线索与会员
- [x] `GET /api/leads`
- [x] `POST /api/leads`
- [x] `GET /api/leads/{id}`
- [x] `PUT /api/leads/{id}`
- [x] `POST /api/leads/{id}/follows`
- [x] `POST /api/leads/{id}/convert-member`
- [x] `GET /api/members`
- [x] `POST /api/members`
- [x] `GET /api/members/{id}`
- [x] `PUT /api/members/{id}`
- [x] `GET /api/members/{id}/timeline`

### F.3 套餐与会员资产
- [ ] `GET /api/packages`
- [ ] `POST /api/packages`
- [ ] `PUT /api/packages/{id}`
- [ ] `GET /api/members/{id}/packages`
- [ ] `GET /api/members/{id}/package-ledgers`

### F.4 教练、时段、预约
- [x] `GET /api/coaches`
- [x] `POST /api/coaches`
- [x] `GET /api/coaches/{id}/slots`
- [x] `POST /api/coaches/{id}/slots`
- [x] `GET /api/slots/available`
- [x] `POST /api/reservations`
- [x] `GET /api/reservations`
- [x] `GET /api/reservations/{id}`
- [x] `POST /api/reservations/{id}/cancel`

### F.5 签到与课消
- [x] `POST /api/checkins`
- [x] `GET /api/checkins`
- [x] `POST /api/consumptions`
- [x] `GET /api/consumptions`
- [x] `POST /api/consumptions/{id}/reverse`

### F.6 收银、支付、退款
- [x] `POST /api/orders`
- [x] `GET /api/orders`
- [x] `GET /api/orders/{id}`
- [x] `POST /api/payments/alipay/precreate`
- [x] `POST /api/payments/alipay/callback`
- [x] `POST /api/refunds`
- [x] `POST /api/refunds/{id}/approve`
- [x] `POST /api/refunds/{id}/reject`
- [x] `GET /api/reconciliations/daily`

### F.7 提成与报表
- [x] `GET /api/commission/rules`
- [x] `POST /api/commission/rules`
- [x] `POST /api/commission/statements/generate`
- [x] `POST /api/commission/statements/{id}/lock`
- [x] `GET /api/commission/statements`
- [x] `GET /api/reports/overview`
- [x] `GET /api/reports/attendance`
- [x] `GET /api/reports/finance`

### F.8 系统与审计
- [x] `GET /api/settings/store`
- [x] `PUT /api/settings/store`
- [x] `GET /api/audit/logs`

## 6 技术规格（PRD）
- [x] Spring Boot 多模块工程搭建
- [x] OpenAPI 文档（`/v3/api-docs`）
- [x] MySQL + Flyway 本地环境（`docker-compose.yml` + profile）
- [x] 核心模块仓储接口化（CRM/排班预约/财务/提成/报表）
- [x] mysql profile 下对应 MyBatis 持久化实现
- [x] 签到与课消持久化（已完成仓储接口化与MySQL实现）
- [ ] RocketMQ 预约事件（未开始）
- [ ] Redis 锁并发控制（未开始）

## 9 验收标准（当前对应状态）
- [x] 核心链路可本地端到端跑通（模拟链路）
- [x] 回调/课消具备幂等处理
- [x] 关键写操作审计留痕
- [ ] 支付、退款、课消账务“数据库级一致性”完全验收（进行中）
- [ ] 无越权漏洞专项测试（未开始）
