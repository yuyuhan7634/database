## 一、项目概述

### 1.1 项目背景

本系统面向高校房产科，实现教工住房的**分房、调房、退房和咨询统计**功能。系统支持**管理员（房产科工作人员）**和**普通住户**两种角色，各自拥有不同的功能界面和操作权限。

### 1.2 核心功能矩阵

| 功能模块 | 管理员 | 住户 | 说明 |
|----------|:--:|:--:|------|
| 系统概览 / 个人信息 | ✅ | ✅ | 管理员看全景统计，住户看个人住房信息 |
| 提交申请 | — | ✅ | 分房/调房/退房三种类型 |
| 申请列表 | ✅（全部+审批） | ✅（仅自己+撤回） | 审批含分房/调房/退房/驳回 |
| 分房队列 | ✅ | — | 按分数排序，手动批量分房 |
| 房产列表 | ✅（CRUD） | ✅（只读） | 新增/编辑/删除房屋 |
| 空闲房产 | ✅（含删除） | ✅（只读） | |
| 住房标准 | ✅ | ✅ | 面积→最低分数对照 |
| 住户列表 | ✅ | — | |
| 房租账单 | ✅ | — | 生成月度账单 |
| 统计报表 | ✅ | — | 部门/职称/面积三维统计 |
| 阈值管理 | ✅ | — | 修改住房标准最低分数 |
| 租金管理 | ✅ | — | 调整房屋每平米租金 |

---

## 二、系统架构

```
┌──────────────────────────────────────────────────────────┐
│                  前端 (Vue 3 + Element Plus)              │
│   localhost:3000                                         │
│   ├── useUser.js composable（登录/注册/角色管理）          │
│   ├── router/index.js（beforeEach 路由守卫）              │
│   ├── ResultDialog.vue（公共组件，消除 8 处重复）          │
│   └── global.css（全局样式，消除 12 处重复）               │
└──────────────┬───────────────────────────────────────────┘
               │  HTTPS (TLS 1.2 自签名证书) + Axios
               ▼
┌──────────────────────────────────────────────────────────┐
│               后端 (Spring Boot 3.5.5, port 8443)        │
│   ├── @RestControllerAdvice 全局异常拦截                  │
│   ├── Controller → Service → Mapper 三层架构              │
│   ├── ScoreCalculator（分数计算，双 Service 共用）         │
│   └── StatisticsService（统计报表，独立 Service）          │
└──────────────┬───────────────────────────────────────────┘
               │  JDBC (MyBatis-Plus 3.5.11)
               ▼
┌──────────────────────────────────────────────────────────┐
│              MySQL 8.0 (estate_management)                │
│   5 张表：house_standard / house / resident /             │
│           rent_bill / application                        │
└──────────────────────────────────────────────────────────┘
   ▲
   │  嵌入 Redis（随应用自动启停，无需外部安装）
   │  用途：@Cacheable 缓存 + Redisson 分布式锁
```

---

## 三、技术栈

### 3.1 后端

| 技术 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.5.5 | 核心框架 |
| MyBatis-Plus | 3.5.11 | ORM（纯注解，零 XML） |
| MySQL Connector | 8.x | 数据库驱动 |
| Spring Data Redis | — | 缓存抽象 |
| Redisson | 3.45.0 | 分布式锁 |
| embedded-redis | 1.4.3 | 嵌入内存 Redis |
| Hutool | 5.8.34 | AES 加密 |
| Lombok | — | 简化实体类 |

### 3.2 前端

| 技术 | 版本 | 用途 |
|------|------|------|
| Vue 3 | 3.4+ | 前端框架 |
| Element Plus | 2.7+ | UI 组件库 |
| Vue Router | 4.3+ | 路由 + 导航守卫 |
| Axios | 1.7+ | HTTP 请求 |
| Vite | 5.4+ | 构建工具 |

---

## 四、后端项目结构

```
Demo/src/main/java/org/example/cloud/demo/
├── EstateApplication.java             12行  启动类 @EnableScheduling
├── common/
│   ├── Result.java                    27行  统一响应体 {code, msg, data}
│   └── GlobalExceptionHandler.java    22行  @RestControllerAdvice 异常拦截
├── config/
│   ├── AsyncConfig.java               32行  多线程线程池 (4核/8最大)
│   ├── EmbeddedRedisConfig.java       40行  嵌入Redis自动启停
│   ├── RedisConfig.java               84行  Redis缓存 (条件启用)
│   └── RedissonConfig.java            35行  Redisson分布式锁 (条件启用)
├── controller/
│   ├── AdminController.java          134行  /admin/*
│   ├── ApplicationController.java     70行  /application/*
│   ├── HouseController.java           54行  /house/*
│   └── ResidentController.java        49行  /resident/*
├── dto/
│   └── ApplicationSubmitDTO.java      19行  申请提交DTO
├── entity/
│   ├── Application.java               30行
│   ├── House.java                     14行
│   ├── HouseStandard.java             16行
│   ├── RentBill.java                  19行
│   └── Resident.java                  21行
├── mapper/                             45行  5个 MyBatis-Plus Mapper
├── service/
│   ├── ApplicationService.java       618行  核心业务（提交/审批/驳回/撤回/批量）
│   ├── HouseService.java              95行  房屋+标准管理
│   ├── ResidentService.java          170行  住户+账单+AES加密
│   ├── ScoreCalculator.java           27行  分数计算（双Service共用）
│   └── StatisticsService.java         97行  统计报表（独立拆分）
└── util/
    ├── AesUtil.java                   53行  AES-128加解密
    └── DistributedLockUtil.java      105行  分布式锁 (Redis/本地回退)
```

---

## 五、前端项目结构

```
前端代码/src/
├── api/                          — API请求层 (23个函数)
│   ├── request.js                — Axios封装 + 拦截器
│   ├── admin.js                  — 7个函数
│   ├── application.js            — 9个函数
│   ├── house.js                  — 3个函数
│   └── resident.js               — 4个函数
├── assets/
│   └── global.css                — 全局公共样式
├── components/
│   └── ResultDialog.vue          — 公共操作结果弹窗
├── composables/
│   └── useUser.js                — 登录/注册/角色状态管理
├── router/
│   └── index.js                  — 14条路由 + beforeEach守卫
└── views/                        — 14个页面组件
    ├── Layout.vue                — 侧边栏 + 角色菜单
    ├── login/Login.vue           — 登录/注册
    ├── dashboard/Dashboard.vue   — 管理员概览 / 住户个人信息
    ├── application/              — ApplicationSubmit / ApplicationList / AllocationQueue
    ├── house/                    — HouseList / AvailableHouses / StandardList
    ├── resident/                 — ResidentList / BillList
    └── admin/                    — Statistics / ThresholdManage / RentManage
```

---

## 六、核心业务设计

### 6.1 分房流程

```
用户提交分房申请
    ↓
校验：必填字段 / 是否已入住 / 申请类型合法
    ↓
ScoreCalculator.calculate(职称, 家庭人口)
    ↓
分数 ≥ 申请面积对应阈值？── 否 → 拒绝（提示分数不足）
    ↓ 是
写入 application 表（apply_status=0, 按 score DESC 排序）
    ↓
每月最后一天 23:00 @Scheduled 触发 / 管理员手动触发
    ↓
DistributedLockUtil 获取分布式锁
    ↓
多线程并行处理（CompletableFuture + 线程池）
  每线程独立事务（TransactionTemplate）
  ↓
  findBestMatchHouse(面积, 分数) → FOR UPDATE 行锁
  ↓ 找到
  allocateHouseToApplicant
  ├── application.apply_status = 1
  ├── house.status = 1
  ├── residentMapper.insert（AES加密敏感字段）
  ├── rentBillMapper.insert
  └── 输出 AllocationResult.toSlip()
```

**分数计算规则**：

| 职称 | 基础分 |
|------|:--:|
| 教授 / 处长 | 100 |
| 副教授 | 80 |
| 科长 / 研究员 | 80 |
| 讲师 / 工程师 | 60 |
| 实验员 / 干事 | 50 |
| 其他 | 40 |

**最终分数 = 基础分 + 家庭人口 × 10**

### 6.2 退房流程

```
管理员审批退房申请
    ↓
校验：申请存在 / 未处理 / 类型=退房
    ↓
getResidentByName → 校验住户存在 / 房号匹配 / 部门匹配
    ↓
house.status = 0（回收）
deleteBillsByNameAndHouse（清理账单）
removeResident（删除住户）
application.applyStatus = 1
```

### 6.3 调房流程

```
管理员审批调房申请
    ↓
校验：申请存在 / 未处理 / 类型=调房
    ↓
getResidentByName → 校验住户存在 / 房号匹配 / 原面积匹配
    ↓
oldHouse.status = 0（退还原房）
    ↓
findBestMatchHouse → 找新房（同分房算法）
    ↓
新房 status = 1
更新 resident（新房号/面积/职称/人口/分数）
deleteBillsByNameAndHouse（清旧账单）
createRentBill（生成新账单）
application.applyStatus = 1
```

---

## 七、权限控制

| 层面 | 实现方式 |
|------|----------|
| 会话 | `sessionStorage`（关浏览器即失效，每次重新登录） |
| 注册 | 用户注册数据持久化在 `localStorage`，含密码校验 |
| 前端路由 | `router.beforeEach` 校验登录 + `meta.requireAdmin` 校验角色 |
| 前端菜单 | Layout.vue 中 `v-if="isAdmin"` 控制管理菜单显示 |
| 前端数据 | 住户申请列表按 `applicantName` 过滤，只显示自己的 |

---

## 八、加分项实现

| # | 加分项 | 实现 | 关键代码 |
|:--:|--------|------|----------|
| 1 | **事务管理** | 10 处 `@Transactional`，覆盖全部分房/退房/调房/驳回/撤回 | ApplicationService + ResidentService |
| 2 | **Redis 缓存** | 嵌入 Redis + `@Cacheable/@CacheEvict`，TTL 30min | EmbeddedRedisConfig |
| 3 | **分布式锁** | Redisson 分布式锁 + 本地 `synchronized` 回退 | DistributedLockUtil |
| 4 | **HTTPS** | keytool 自签名证书 + Spring Boot SSL (8443) | keystore.p12 |
| 5 | **多线程** | 线程池(4/8) + CompletableFuture + TransactionTemplate | AsyncConfig |
| 6 | **数据加密** | AES-128 加密 Resident.ownerName/department | AesUtil |

---

## 九、API 接口一览

| 方法 | 端点 | 说明 |
|------|------|------|
| GET | `/application/list` | 查询所有申请单 |
| GET | `/application/queue` | 查询分房队列（分数降序） |
| POST | `/application/submit` | 提交申请 |
| POST | `/application/approve` | 审批分房 |
| POST | `/application/checkout` | 审批退房 |
| POST | `/application/transfer` | 审批调房 |
| POST | `/application/reject` | 驳回申请 |
| POST | `/application/cancel` | 撤回申请 |
| POST | `/application/allocate/monthly` | 手动批量分房 |
| GET | `/house/list` | 房产列表 |
| GET | `/house/available` | 空闲房产 |
| GET | `/house/standard/list` | 住房标准列表 |
| GET | `/resident/list` | 住户列表 |
| GET | `/resident/bill/list` | 房租账单 |
| POST | `/resident/bill/generate` | 生成月度账单 |
| POST | `/resident/updateInfo` | 更新个人信息 |
| GET | `/admin/statistics` | 统计报表 |
| GET | `/admin/threshold` | 阈值查询 |
| POST | `/admin/updateStandard` | 修改阈值 |
| POST | `/admin/updateRent` | 调整租金 |
| POST | `/admin/addHouse` | 新增房屋 |
| POST | `/admin/updateHouse` | 修改房屋 |
| POST | `/admin/deleteHouse` | 删除房屋 |

---

## 十、运行说明

### 环境要求

- JDK 21 · Node.js 18+ · MySQL 8.0
- 数据库名 `estate_management`，字符集 `utf8mb4`
- Redis **无需安装**（嵌入内存自动启停）

### 启动步骤

```bash
# 1. 创建数据库
mysql -u root -p -e "CREATE DATABASE IF NOT EXISTS estate_management CHARACTER SET utf8mb4"

# 2. 初始化数据
mysql -u root -p estate_management < 数据库/schema.sql
mysql -u root -p estate_management < 数据库/data.sql

# 3. 启动后端
cd 后端服务代码/Demo
./mvnw spring-boot:run

# 4. 启动前端（新终端）
cd 前端代码
npm install
npm run dev

# 5. 访问 http://localhost:3000
```

### 默认账户

| 角色 | 用户名 | 密码 |
|------|--------|------|
| 管理员 | admin | 123456 |
| 住户 | 注册创建 | 自设 |
