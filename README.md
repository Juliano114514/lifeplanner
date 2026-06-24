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

**一期实现范围**：一天规划链路（首页 → 规划流 → 结果页）已落地；库存统计模块见 §5.3 PRD，代码尚未接入。

UI 视觉与交互规范见 [`项目ui交互语言设计.md`](项目ui交互语言设计.md)。

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
│       │   ├── model/
│       │   │   ├── PlanCardType.kt             // 卡片类型枚举
│       │   │   ├── PlanCardAnswer.kt           // 单卡答案（含 subSelection）
│       │   │   └── PlanRecord.kt               // 规划主记录
│       │   └── plan/
│       │       ├── PlanCardCatalog.kt          // 8 张卡定义与选项
│       │       ├── PlanCardDefinition.kt       // 卡元数据 + FollowUp 二级选单
│       │       ├── PlanInteraction.kt          // 交互类型枚举
│       │       └── PlanFlowReducer.kt          // 纯函数状态机（next/previous/skip）
│       └── util/
│           ├── PlanTextExporter.kt             // 按时段导出规划摘要
│           └── DateTimeUtil.kt                 // 日期 / 过期判断
│
├── libroom/                                    // Room 数据层（一期已实现规划）
│   └── src/main/java/com/example/libroom/
│       ├── local/
│       │   ├── AppDatabase.kt                  // Room v2 + Migration 1→2
│       │   ├── dao/
│       │   │   └── PlanDao.kt
│       │   ├── entity/                         // plan_record / plan_card_answer
│       │   └── mapper/
│       │       └── PlanMapper.kt
│       └── repository/
│           ├── PlanRepository.kt
│           └── PlanRepositoryFactory.kt
│
├── libui/                                      // Compose 展示层（无业务编排）
│   └── src/main/java/com/example/libui/
│       ├── theme/
│       │   ├── Dimens.kt                       // 间距 / 圆角 / 厚度 / Motion token
│       │   ├── Type.kt                         // 字体层级
│       │   └── Theme.kt                        // Material3 色板注入
│       ├── components/
│       │   ├── TactileSurface.kt               // 3D 按压地基
│       │   ├── ChunkyButton.kt                 // 厚主按钮
│       │   ├── ChunkyChip.kt                   // 选项 chip
│       │   ├── ChunkyCard.kt                   // 首页入口卡
│       │   ├── TagChoiceGroup.kt               // chip 流式布局
│       │   ├── PlanCardFrame.kt                // 规划卡外框
│       │   ├── PlanProgressBar.kt              // 分段进度条
│       │   ├── HourTimePicker.kt               // 归家时间
│       │   └── ConfettiBurst.kt                // 结果页撒花
│       └── feature/
│           ├── home/
│           │   ├── HomeContent.kt
│           │   └── HomeContract.kt
│           └── plan/
│               ├── PlanContent.kt              // 规划卡片流
│               ├── PlanContract.kt             // PlanUiState / PlanAction
│               ├── PlanResultContent.kt
│               ├── PlanResultContract.kt
│               └── cards/
│                   └── PlanCardBodies.kt       // TagCard / OtherNoteCard
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

> Room 当前版本：**v2**（`AppDatabase`）。v1→v2 迁移：`plan_card_answer` 新增 `sub_selection` 列。

#### `plan_record` — 规划主记录

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `date` | TEXT (INDEXED) | 规划日期，`yyyy-MM-dd` |
| `created_at` | INTEGER | 创建时间戳 (ms) |
| `completed_at` | INTEGER? | 完成时间戳，null = 未完成（中断） |
| `current_index` | INTEGER | 中断恢复：当前卡片索引 (0-based) |
| `fitness_step` | TEXT | **遗留列**，域层已不再使用；二级选单改由 `plan_card_answer.sub_selection` 存储 |
| `export_text` | TEXT? | 完成时导出的纯文本快照 |

#### `plan_card_answer` — 单张卡片答案

| 字段 | 类型 | 说明 |
|---|---|---|
| `id` | INTEGER (PK, auto) | 主键 |
| `plan_record_id` | INTEGER (FK → plan_record) | 所属规划 |
| `card_type` | TEXT | `GO_OUT` / `WORK` / `FITNESS` / `BREAKFAST` / `LUNCH` / `DINNER` / `RETURN_HOME` / `OTHER` |
| `card_index` | INTEGER | 卡片顺序位置 (0-based) |
| `selected_options` | TEXT | 主选项，JSON 数组，如 `["早","下午"]` 或 `["自己做"]` |
| `sub_selection` | TEXT? | 条件二级选单，如健身强度 `低`、三餐 `没菜去买菜` |
| `slider_value` | REAL? | **遗留列**，域层已不再使用 |
| `time_value` | TEXT? | 归家时间，`HH:mm` |
| `note_text` | TEXT? | 「其他」主备注 |
| `extra_notes` | TEXT? | 额外备注列表，JSON 数组 |

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

> UI 交互与视觉规范见 [`项目ui交互语言设计.md`](项目ui交互语言设计.md)。

#### 领域模型（`foundation/domain/plan`）

| 类型 | 职责 |
|---|---|
| `PlanCardCatalog` | 8 张卡的标题、选项、`FollowUp` 配置 |
| `PlanCardDefinition` | 单卡元数据：`interaction`、互斥项、`followUp` |
| `FollowUp` | 条件二级选单（主选触发后在**同卡内**展开，不切换整卡） |
| `PlanFlowReducer` | 纯函数状态机：`toggleTag` / `selectSingle` / `toggleFollowUp` / `next` / `previous` / `skip` |
| `PlanFlowState` | `currentIndex` + `answers: Map<PlanCardType, PlanCardAnswer>` |

#### 卡片流（共 8 张）

| # | 卡片 | 交互 | 主选项 | 条件二级（FollowUp） |
|---|---|---|---|---|
| 1 | 早餐 | 单选 Tag | `自己做` `出去吃` `外卖` `不吃` | **怎么做**（仅「自己做」，多选）：`有菜了` `要去买菜` `要外卖点菜` |
| 2 | 午餐 | 单选 Tag | 同上 | 同上 |
| 3 | 晚餐 | 单选 Tag | 同上 | 同上 |
| 4 | 出门 | 多选 Tag | `早上` `下午` `晚上` `不出门` | **出门做什么**（非「不出门」，多选）：`出去玩` `出去办事` `出去团建` |
| 5 | 学习 / 工作 | 多选 Tag | `早上` `下午` `晚上` `休息` | **在哪里**（非「休息」，多选）：`在家` `北区` `研究生部` `出差` |
| 6 | 健身 | 多选 Tag | `早上` `下午` `晚上` `今日练休` | **强度**（多选）：`低` `中` `高`（「今日练休」时跳过） |
| 7 | 晚上回家 | 时间选择 | 滑动选择预计到家小时（6–23） | — |
| 8 | 其他 | 备注 | 主备注 +「添加备注」追加多条 | — |

- **二级选单规则**：主选满足 `FollowUp.triggers`（空集表示任意主选触发）且未命中 `skip` 时，卡内纵向展开二级区；答案写入 `subSelections`（JSON 数组，存于 `sub_selection` 列）。
- **跳过**：任意卡片可跳过，该卡答案置空并前进。
- **回退**：顶栏「上一张」+ 下滑手势；换卡导航 250ms 防抖。
- **中断恢复**：每次答题与换卡写入 Room；当日未完成记录在 12h 内可恢复 `current_index` 与已有答案。

#### 结果页

- `PlanTextExporter` 按 **早晨 → 下午 → 晚上 → 其他** 生成摘要；餐食含二级时格式为 `早餐：自己做 · 没菜去买菜`。
- 操作：**一键复制到剪贴板** + 返回首页（一期纯文本，不做图片导出）。

### 5.3 模块二：库存统计（PRD · 待实现）

> 以下为目标能力描述；`libroom` / `libui` 中尚未包含库存相关 DAO 与 UI。

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
| **卡片流** | 居中单卡 `PlanCardFrame`；选选项后点底部「下一张」/「完成」推进 |
| **进度** | 顶部分段进度条（8 段）+ 卡内标题下 `当前/总数`（如 `2 / 8`） |
| **换卡动画** | 仅 `currentIndex` 变化时横滑；二级 follow-up 卡内展开，不触发整卡动画 |
| **回退** | 顶栏「上一张」+ 下滑手势 |
| **中断恢复** | 规划：`plan_record.current_index` + `plan_card_answer`；盘点：`inventory_session`（12h 过期） |
| **空状态** | 无数据时展示空状态插画 + 引导文案 |

---

## 7. 数据流（MVI）

```
User Action (PlanAction)
    ↓
PlanViewModel.onAction()
    ↓
PlanFlowReducer（纯函数，foundation）
    ↓
PlanRepository 持久化 + publishFlowState()
    ↓
PlanUiState → PlanContent 重组
```

- **State**：`StateFlow<PlanUiState>` 为 UI 单一数据源。
- **Effect**：导航等一次性事件经 `SharedFlow<PlanEffect>` 发出（如跳转结果页）。
- **分层**：领域逻辑在 `foundation`（无 Android 依赖）；`libui` 只消费 `UiState` + 回调 `PlanAction`；`app` 的 `PlanScreen` 连接 ViewModel 与 `PlanContent`。
- **扩展新卡**：在 `PlanCardCatalog` 追加定义；若需二级，配置 `followUp` 即可，通常无需改 UI。

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
| 7 | 健身 / 餐食二级选单？ | **同卡内 FollowUp 展开**，存 `sub_selection`；不再使用子步骤整卡切换 |

---

## 9. 版本路线

| 版本 | 范围 |
|---|---|
| v1.0 | 一天规划卡片流 + 库存 CSV 导入 + 盘点卡片流 + 纯文本导出 + 中断恢复 + 手动增删物品 |
| v1.1 | 图片导出（Compose Canvas 绘制日程海报） |
| v2.0 | 盘点趋势图表、历史对比、消耗速率预估 |
