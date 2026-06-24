# 居家规划与库存管理 App PRD（v1.0）

---

## 1. 产品概述

| 维度 | 说明 |
|---|---|
| **定位** | 轻量级个人生活管理工具，以"刷卡片"的极简交互完成当日日程规划与周期性库存清点 |
| **核心痛点** | 传统表单输入繁琐、用户难以坚持；卡片式单选题最大程度降低填写阻力与决策疲劳 |
| **目标用户** | 有日程规划与居家物品管理需求的个人用户 |
| **平台** | Android（一期仅手机端） |

---

## 2. 技术栈

| 层级 | 选型 | 理由 |
|---|---|---|
| UI | Jetpack Compose | 声明式 UI，天然适配卡片流与动画 |
| 架构 | MVI | 单向数据流，状态可追溯，便于中断恢复 |
| DI | Koin | 轻量，Kotlin 原生 DSL，无注解处理器 |
| 本地存储 | Room | 类型安全的 SQLite 封装，支持 Flow 响应式查询 |
| 数据导入 | CSV 解析（opencsv / kotlin-csv） | 用户通过 CSV 批量导入物品清单 |
| 构建 | Gradle KTS + Version Catalog | 现代化依赖管理 |

---

## 3. 纯文本项目结构图

### 3.1 模块总览与依赖关系

```
                    ┌─────────────┐
                    │     app     │  Android Application（壳层）
                    └──────┬──────┘
           ┌───────────────┼───────────────┐
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │    libui    │ │   libroom   │ │ foundation  │
    │  UI 展示层  │ │  Room 数据层 │ │ 纯 Kotlin   │
    └──────┬──────┘ └──────┬──────┘ └─────────────┘
           │               │
           └───────┬───────┘
                   ▼
            ┌─────────────┐
            │ foundation  │  领域模型 / 工具 / CSV（无 Android 依赖）
            └─────────────┘
```

| 模块 | 类型 | 职责 | 依赖 |
|---|---|---|---|
| `foundation` | JVM Library | 领域模型、CSV 解析、纯文本导出、时间工具 | — |
| `libroom` | Android Library | Room Entity / DAO / Database / Repository | `foundation` |
| `libui` | Android Library | 设计主题、通用组件、无状态 Feature UI（`*Content`：仅 `UiState` + 回调，无 ViewModel / Repository） | `foundation` |
| `app` | Android Application | Activity、导航、Koin DI、ViewModel、薄包装 Screen（`collect` 状态后委托 `*Content`） | `foundation`, `libroom`, `libui` |

版本与第三方依赖统一维护于 `gradle/libs.versions.toml`（Version Catalog）。

**UI 分层约定**：`libui` 提供 `XxxContent(state, onAction)`；`app` 的 `XxxScreen` 负责注入 ViewModel、收集 `StateFlow`、处理导航与一次性 Effect。ViewModel 与 `NavGraph` 不进入 `libui`，以保持 `libui → foundation` 的依赖方向（不依赖 `libroom`）。

### 3.2 目录结构

```
lifeplanner/
├── app/                                        // 应用壳层（编排）
│   ├── src/main/java/com/example/lifeplanner/
│   │   ├── App.kt                              // Application，Koin 初始化
│   │   ├── MainActivity.kt                     // 单 Activity，Compose 入口
│   │   ├── di/
│   │   │   └── AppModule.kt                    // Repository / ViewModel / DAO 注册
│   │   └── ui/
│   │       ├── navigation/
│   │       │   └── NavGraph.kt                 // 导航图（路由注册与 Screen 组装）
│   │       ├── home/
│   │       │   ├── HomeScreen.kt               // 薄包装：ViewModel → HomeContent
│   │       │   └── HomeViewModel.kt
│   │       ├── plan/
│   │       │   ├── PlanScreen.kt               // 薄包装：ViewModel → PlanContent
│   │       │   ├── PlanResultScreen.kt         // 薄包装：ViewModel → PlanResultContent
│   │       │   └── PlanViewModel.kt
│   │       └── inventory/
│   │           ├── CategoryListScreen.kt       // 薄包装 → CategoryListContent
│   │           ├── InventoryScreen.kt          // 薄包装 → InventoryContent
│   │           ├── InventoryResultScreen.kt    // 薄包装 → InventoryResultContent
│   │           └── InventoryViewModel.kt
│   └── src/test/                               // 单元测试
│
├── foundation/                                 // 纯 Kotlin JVM 模块
│   └── src/main/java/com/example/foundation/
│       ├── domain/
│       │   └── model/
│       │       ├── CardItem.kt                 // 卡片数据模型（密封类）
│       │       ├── PlanResult.kt               // 规划结果
│       │       └── InventorySnapshot.kt        // 盘点快照
│       ├── csv/
│       │   └── CsvParser.kt                    // CSV 解析与校验
│       └── util/
│           ├── TextExporter.kt                 // 纯文本导出
│           └── DateTimeUtil.kt                 // 时间工具
│
├── libroom/                                    // Room 数据层
│   └── src/main/java/com/example/libroom/
│       ├── local/
│       │   ├── AppDatabase.kt                  // Room Database
│       │   ├── dao/
│       │   │   ├── PlanDao.kt                  // 一天规划 DAO
│       │   │   ├── CategoryDao.kt              // 库存大类 DAO
│       │   │   ├── InventoryItemDao.kt         // 库存物品 DAO
│       │   │   └── InventoryRecordDao.kt       // 盘点记录 DAO
│       │   └── entity/                         // Room Entity（见 §4）
│       └── repository/
│           ├── PlanRepository.kt               // 规划数据仓库
│           └── InventoryRepository.kt          // 库存数据仓库
│
├── libui/                                      // Compose 展示层（无业务编排）
│   └── src/main/java/com/example/libui/
│       ├── theme/                              // Material3 主题
│       │   ├── Color.kt
│       │   ├── Type.kt
│       │   └── Theme.kt
│       ├── components/                         // 跨 Feature 复用组件
│       │   ├── CardStack.kt                    // 卡片堆叠容器
│       │   ├── ProgressBar.kt                  // 进度指示条
│       │   ├── StatusSegmentedButton.kt        // 缺/较少/足够/过多 状态组
│       │   └── QuantitySlider.kt               // 数量滑动条（带吸附）
│       └── feature/                            // 无状态 Feature UI
│           ├── home/
│           │   └── HomeContent.kt              // 首页（两大入口）
│           ├── plan/
│           │   ├── PlanContent.kt              // 规划卡片流
│           │   └── PlanResultContent.kt        // 规划结果 & 导出
│           └── inventory/
│               ├── CategoryListContent.kt      // 大类列表
│               ├── InventoryContent.kt         // 盘点卡片流
│               └── InventoryResultContent.kt   // 采购清单 & 导出
│
├── gradle/
│   └── libs.versions.toml                      // 版本号 & 依赖坐标（Version Catalog）
├── build.gradle.kts                            // 根构建脚本（插件版本声明）
└── settings.gradle.kts                         // 模块注册
```

---

## 4. Room 数据库字段设计

### 4.1 表总览

| 表名 | 用途 |
|---|---|
| `plan_record` | 一天的规划记录 |
| `plan_card_answer` | 单张卡片的选择结果 |
| `inventory_category` | 库存大类（日用品 / 厨房用品 …） |
| `inventory_item` | 具体物品（厕纸 / 洗洁精 …） |
| `inventory_record` | 某次盘点某物品的快照 |
| `inventory_session` | 盘点会话（用于中断恢复） |

### 4.2 表结构

#### `plan_record` — 规划主记录

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `date` | TEXT (INDEXED) | 规划日期，`yyyy-MM-dd` |
| `created_at` | INTEGER | 创建时间戳 (ms) |
| `completed_at` | INTEGER? | 完成时间戳，null = 未完成（中断） |
| `export_text` | TEXT? | 最终导出文本快照 |

#### `plan_card_answer` — 单张卡片答案

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `plan_record_id` | INTEGER (FK → plan_record) | 所属规划 |
| `card_type` | TEXT | 卡片类型枚举：`GO_OUT` / `WORK` / `FITNESS` / `BREAKFAST` / `LUNCH` / `DINNER` / `RETURN_HOME` |
| `card_index` | INTEGER | 卡片顺序位置 (0-based) |
| `selected_options` | TEXT | 多选存储，JSON 数组，如 `["早","下午"]` |
| `slider_value` | REAL? | 滑动条数值（仅健身强度等卡片使用） |
| `time_value` | TEXT? | 时间选择器值（仅归家卡片），`HH:mm` |

#### `inventory_category` — 库存大类

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `name` | TEXT (UNIQUE) | 大类名称，如"日用品" |
| `sort_order` | INTEGER | 排序权重 |
| `item_count` | INTEGER | 该大类物品总数（冗余，便于列表展示进度） |
| `checked_count` | INTEGER | 本次已盘点数（冗余） |

> 支持手动新增/编辑/删除大类。

#### `inventory_item` — 物品

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `category_id` | INTEGER (FK → inventory_category) | 所属大类 |
| `name` | TEXT | 物品名称 |
| `unit` | TEXT | 计量单位（卷 / 瓶 / 包 …） |
| `slider_min` | REAL (default 0) | 滑动条下限 |
| `slider_max` | REAL (default 10) | 滑动条上限；缺省与状态组吸附点对齐 |
| `step_type` | TEXT | 步进类型：`SNAP_TO_STATUS`（与状态组吸附）/ `CONTINUOUS`（连续） |
| `last_status` | TEXT? | 上次盘点状态：`LACK` / `LOW` / `ENOUGH` / `EXCESS` |
| `last_quantity` | REAL? | 上次盘点数量 |
| `sort_order` | INTEGER | 排序权重 |
| `is_active` | INTEGER (default 1) | 软删除标记；0 = 已删除 |

> 支持手动新增/编辑/删除物品。CSV 导入时通过 `(category_id, name)` 去重追加。

#### `inventory_session` — 盘点会话

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `category_id` | INTEGER (FK) | 正在盘点的大类 |
| `current_item_index` | INTEGER | 当前进行到的物品索引 |
| `started_at` | INTEGER | 开始时间戳 (ms) |
| `paused_at` | INTEGER? | 暂存 / 中断时间戳 |
| `expires_at` | INTEGER | 过期时间戳（12h 后），过期后丢弃 |

#### `inventory_record` — 盘点记录

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `item_id` | INTEGER (FK → inventory_item) | 物品 |
| `status` | TEXT | `LACK` / `LOW` / `ENOUGH` / `EXCESS` |
| `quantity` | REAL | 盘点数量 |
| `recorded_at` | INTEGER | 记录时间戳 (ms) |

---

## 5. 功能模块

### 5.1 首页

- 极简双卡片入口：
  - **一天规划** → 进入规划卡片流
  - **库存统计** → 进入大类列表

### 5.2 模块一：一天规划

#### 卡片流（共 7 张）

| # | 卡片 | 交互 | 选项 |
|---|---|---|---|
| 1 | 出门 | **多选 Tag** | `早` `下午` `晚上` `不出门`（选"不出门"自动清空其余） |
| 2 | 学习 / 工作 | **多选 Tag** | `早` `下午` `晚上` `休息` |
| 3 | 健身 | **第一步**多选时段 → **第二步**强度 | 时段：`早` `下午` `晚上` `今日练休`；强度：`低` `中` `高` |
| 4 | 早餐 | **单选** | `自己做` `出去吃` `外卖` `不吃` |
| 5 | 午餐 | **单选** | 同上 |
| 6 | 晚餐 | **单选** | 同上 |
| 7 | 归家 | **TimePicker** | 滑动刻度盘，选择预计到家时间（精确到小时就行） |

- **跳过逻辑**：允许跳过任意卡片，跳过的卡片值置空。
- **容错**：提供"上一张"按钮 / 下滑手势回退修改。

#### 结果页

- 按时间线（早 → 下午 → 晚）生成今日行程摘要文本。
- 操作：**一键复制到剪贴板**（一期仅纯文本，不做图片导出）。

### 5.3 模块二：库存统计

#### CSV 导入

- 支持从本地文件选择 `.csv`。
- CSV 格式：`大类,物品名称,计量单位[,slider_min,slider_max,step_type]`
  - 后三个字段可选，缺省时 slider_min=0, slider_max=10, step_type=SNAP_TO_STATUS。
- 导入逻辑：按 `(大类, 物品名称)` 去重，仅追加新物品；已存在的物品更新单位与上下限。

#### 类别列表页

- 展示所有大类卡片，每张显示盘点进度（如 `日用品 已盘点 5/15`）。
- 支持**手动新增大类**、**导出当前库存数据为 CSV**（便于离线编辑后一键去重再导入）。

#### 盘点卡片流

- 进入某大类后，依次展示物品卡片：
  - **状态组**：`缺` `较少` `足够` `过多`（SegmentedButton，四选一）
  - **滑动条**：显示当前数值 + 单位（如"大概剩 5 卷"）
  - **联动规则**：
    - 拖动滑动条到 0 → 自动吸附 `缺`
    - 拖动滑动条到 `slider_max * 0.25` → 吸附 `较少`
    - 拖动滑动条到 `slider_max * 0.5` → 吸附 `足够`
    - 拖动滑动条到 `slider_max` → 吸附 `过多`
    - 手动点选状态组 → 滑动条自动移动到对应吸附点
    - `step_type=CONTINUOUS` 时不吸附，状态组与滑动条互不干涉

#### 采购清单结果页

- 筛选状态为 `缺` 或 `较少` 的物品，生成采购清单文本。
- 支持一键复制。
- 盘点数据同时写入 Room，作为下次盘点的默认初始状态。

#### 新增：手动管理物品

- 物品列表页支持长按编辑 / 左滑删除。
- 删除为软删除（`is_active=0`），CSV 重新导入时可恢复。
- 支持手动新增物品（填写名称、单位、上下限）。

---

## 6. 全局交互规范

| 规范 | 说明 |
|---|---|
| **卡片流** | 居中单卡片渲染，点击选项 / 手势滑动进入下一张 |
| **进度条** | 顶部展示 `当前/总数`（如 `2/8`） |
| **回退** | "上一张"按钮 + 下滑手势 |
| **中断恢复** | 离开卡片流时自动保存进度到 `inventory_session` / `plan_record`；重新进入若未过期（12h）则恢复到中断位置 |
| **空状态** | 无数据时展示空状态插画 + 引导文案 |

---

## 7. 数据流（MVI）

```
User Action
    ↓
[Intent] ──→ ViewModel.reduce()
    ↓
[State] (immutable data class)
    ↓
Compose UI 重组渲染
```

- **State** 单一数据源，ViewModel 持有 `StateFlow<UiState>`。
- **Effect**（一次性事件，如 Toast / 导航跳转）通过 `Channel` 发送。
- Room DAO 返回 `Flow<List<T>>`，ViewModel 中 `flatMapLatest` 合并为 UiState。

---

## 8. QA 汇总（已决议）

| # | 问题 | 决议 |
|---|---|---|
| 1 | CSV 更新机制：重新导入全量覆盖还是 App 内手动增删？ | **支持手动增删 + 导出当前数据为 CSV**，导入时按 `(大类, 物品)` 去重追加 |
| 2 | 滑动条最大值如何确定？ | **`inventory_item` 表预留 `slider_min` / `slider_max` / `step_type`**；缺省 slider_min=0, slider_max=10, step_type=SNAP_TO_STATUS；拖拽时与状态组吸附点对应 |
| 3 | 是否持久化历史记录？ | **是**，规划记录存 `plan_record` + `plan_card_answer`，盘点记录存 `inventory_record` |
| 4 | 中断恢复？ | **支持**，通过 `inventory_session` 记录中断位置，设置 12h 过期 |
| 5 | 卡片流是否允许跳过？ | **允许**，跳过项置空，不强制全填 |
| 6 | 导出格式？ | **一期仅纯文本复制**，二期可拓展图片导出 |

---

## 9. 版本路线

| 版本 | 范围 |
|---|---|
| v1.0 | 一天规划卡片流 + 库存 CSV 导入 + 盘点卡片流 + 纯文本导出 + 中断恢复 + 手动增删物品 |
| v1.1 | 图片导出（Compose Canvas 绘制日程海报） |
| v2.0 | 盘点趋势图表、历史对比、消耗速率预估 |
