# 项目 UI 交互语言设计规范（v1.1）

> **设计语言**：Chunky / Neo-tactile（多邻国式 3D 厚触点）× Material 3 结构与栅格  
> **适用范围**：首页入口 → 一天规划卡片流（8 张）→ 规划结果页；库存链路复用同一组件库  
> **实现模块**：`libui`（展示）+ `foundation/domain/plan`（卡片元数据与流程）  
> **Token 唯一数据源**：`libui/theme/Dimens.kt`、`libui/theme/Type.kt`、`libui/res/values/colors.xml`

---

## 1. 设计原则

| 原则 | 含义 | 落地手段 |
|---|---|---|
| **M3 结构，Chunky 触点** | 布局、间距、层级跟 Material 3；只有可点击元素保留厚度与下沉 | 卡面/进度条用 M3 分段与 tonal surface；按钮/chip 用 `TactileSurface` |
| **可触摸的厚度** | 可点元素有实体感 | 底部硬阴影 + 按下时主体下沉（`TactileSurface`） |
| **即时正反馈** | 操作有轻量响应，不叠帧 | 选中勾渐显；换卡横滑 ≤240ms；**禁止** chip 持续 scale 弹簧 |
| **一次一焦点** | 卡片流居中单卡，干扰最小 | 大留白 + 内容区 `weight(1f)` 光学居中 |
| **手势可回退** | 误操作零成本 | 下滑回退 + 顶栏「上一张」图标按钮 |
| **二级不整卡切换** | 条件 follow-up 在同卡内展开 | `AnimatedVisibility` 纵向展开，不触发 `AnimatedContent` 横滑 |

视觉血统：多邻国（厚按钮 + 撒花 + 按压下沉）× M3 Expressive 间距节奏。品牌色 `#00DC5A`。

---

## 2. 设计 Token

### 2.1 间距与圆角 — `theme/Dimens.kt`

对齐 **8dp 栅格**；组件只引用 token，禁止散落魔法数字。

| Token | 值 | 用途 |
|---|---|---|
| `space4` | 4dp | chip 勾与文字间距 |
| `space8` | 8dp | 组件内小间距、`gapTag` |
| `space12` | 12dp | 二级标题与选项区间距 |
| `space16` | 16dp | `screenPadding`、chip 水平内边距 |
| `space24` | 24dp | `cardPadding`、区块间距 |
| `radiusChip` | 16dp | 胶囊 chip |
| `radiusButton` | 16dp | 厚按钮 |
| `radiusCard` | 24dp | 卡面（M3 Extra Large） |
| `depthButton` | 4dp | 按钮下沉行程 |
| `depthCard` | 4dp | 卡片底影偏移 |
| `depthChip` | 3dp | chip 选中底影 |

### 2.2 动效 — `theme/Dimens.kt` → `Motion`

| Token | 值 (ms) | 用途 |
|---|---|---|
| `Motion.FAST` | 120 | 按压下沉、退场淡出 |
| `Motion.MEDIUM` | 240 | 换卡横滑、进度段变色、二级展开 |
| `Motion.SLOW` | 400 | 撒花等长动效（少用） |

**动效分层（强制）**：

| 层级 | 触发 | 实现 | 禁止 |
|---|---|---|---|
| L1 页面 | `currentIndex` 变化 | `AnimatedContent` + `contentKey` | 用闭包读外层 `state`；把 follow-up 放进 key |
| L2 卡内 | 主选触发二级 | `AnimatedVisibility` 纵向展开 | 整卡横滑 |
| L3 控件 | chip 选中 | 勾 `fadeIn` + `expandHorizontally` | `scale` 弹簧、`spring` 常驻动画 |

换卡 `transitionSpec` 标准：

```kotlin
slideInHorizontally(tween(Motion.MEDIUM)) { dir * it / 4 } + fadeIn(tween(Motion.MEDIUM))
  togetherWith
slideOutHorizontally(tween(Motion.MEDIUM)) { -dir * it / 4 } + fadeOut(tween(Motion.FAST))
```

导航防抖：`PlanViewModel` 对 `Next` / `Previous` / `Skip` 限流 **250ms**。

### 2.3 颜色 — `res/values/colors.xml`

| 角色 | 浅色 | 用途 |
|---|---|---|
| `theme_primary` | `#00DC5A` | 品牌主色 |
| `theme_primary_light` | `#F0E5FBF2` | `primaryContainer` |
| `bg_page` | `#F2F4F7` | 页面底 |
| `bg_card` | `#F2FFFFFF` | 卡面（半透叠层） |
| `depth_primary` | `#00A848` | 主按钮/chip 底影 |
| `depth_card` | `#14000000` | 卡片底影 |

暗色覆盖见 `values-night/colors.xml`（`depth_*` 单独加深）。

### 2.4 字体 — `theme/Type.kt`

系统默认字体，靠字重拉开层级：

| 角色 | style | size / weight | 用途 |
|---|---|---|---|
| 屏标题 | `headlineMedium` | 28sp / 800 | 首页、结果页标题 |
| 卡片标题 | `titleLarge` | 20sp / 700 | `PlanCardFrame` 居中标题 |
| 步骤 | `labelMedium` | — | `2 / 8` 副标题 |
| 按钮 | `titleMedium` | 17sp / 700 | `ChunkyButton` |
| 选项 | `labelLarge` | 16sp / 600 | chip 文本 |
| 二级标题 | `SectionTitleStyle` | 14sp / 700 | follow-up 区标题（如「怎么做」） |
| 正文 | `bodyLarge` | 16sp | 结果摘要 |

---

## 3. 核心组件目录

| 组件 | 路径 | 职责 |
|---|---|---|
| `TactileSurface` | `components/TactileSurface.kt` | 3D 按压地基：底影 + `offsetY` 下沉 |
| `ChunkyButton` | `components/ChunkyButton.kt` | 主操作按钮（下一张 / 完成 / 复制） |
| `ChunkyChip` | `components/ChunkyChip.kt` | 单/多选标签 |
| `ChunkyCard` | `components/ChunkyCard.kt` | 首页入口卡 |
| `TagChoiceGroup` | `components/TagChoiceGroup.kt` | 双列栅格包裹多个 `ChunkyChip`（每行最多 2 项） |
| `PlanCardFrame` | `components/PlanCardFrame.kt` | 规划卡外框 + 顶栏 |
| `PlanProgressBar` | `components/PlanProgressBar.kt` | 8 段分段进度 |
| `HourTimePicker` | `components/HourTimePicker.kt` | 归家时间选择 |
| `ConfettiBurst` | `components/ConfettiBurst.kt` | 结果页撒花 |
| `TagCard` | `feature/plan/cards/PlanCardBodies.kt` | 主选 + 条件二级（follow-up） |

---

## 4. 组件交互规格

### 4.1 TactileSurface / ChunkyButton

- 静止：主体在上，下方露出 `depth` 深色底块；`padding(bottom = depth)` 预留恒定高度，**按压不引起兄弟节点抖动**。
- 按下：`animateDpAsState` 驱动 `bodyOffset` 0 → `depth`，`Motion.FAST`，无 ripple。
- `ChunkyButton`：高 54dp，`Primary` 品牌绿 + `depth_primary`；`Outline` 描边 + `depth_surface`。

### 4.2 ChunkyChip

- 布局：双列栅格内 `fillMaxWidth` + 内容 `Row` 居中；选中时对勾 `fadeIn`，**不用** `expandHorizontally`，外框宽度不变。
- 未选：`surface` 填充 + 1dp `outline` 描边，无底影。
- 选中：`primaryContainer` + 2dp `primary` 描边 + `depthChip` 底影；左侧 `Check` 图标 `AnimatedVisibility` 渐显。
- **不做** `scale` 弹跳（避免换卡时多 chip 叠帧卡顿）。

### 4.3 PlanCardFrame

```
┌─────────────────────────────────────┐
│  ←          早餐              ↷     │  圆形 IconButton
│              2 / 8                  │  居中 titleLarge + labelMedium
├─────────────────────────────────────┤
│                                     │
│         [ 选项区 weight(1f) ]        │  内容光学靠上，卡面 fillMaxSize
│                                     │
└─────────────────────────────────────┘
  └─ depthCard 硬阴影（卡面必须 fillMaxSize，避免露出灰底）
```

- 圆角 `radiusCard`，`surface` 填充，`Column.fillMaxSize()`。
- 内容区：`Box(weight(1f))` 包裹 slot。
- 下滑 `detectVerticalDragGestures`（`dragAmount > 24`）触发回退，需 `canGoPrevious`。

### 4.4 PlanProgressBar

- 8 段（与 `PlanCardCatalog.TOTAL_COUNT` 一致），段间距 `space4`，高 6dp，圆角 50%。
- `index <= currentIndex` 的段填 `primary`，其余 `surfaceVariant`；`animateColorAsState(tween(Motion.MEDIUM))`。

### 4.5 TagCard（主选 + 二级 follow-up）

- 主选：`TagChoiceGroup` 双列（`MULTI_TAG` / `SINGLE_TAG`）。
- 二级：当 `PlanCardDefinition.activeFollowUp(selected)` 非空时，`AnimatedVisibility(expandVertically + fadeIn)` 展示 `SectionTitleStyle` 标题 + 单选 `TagChoiceGroup`。
- 出门 / 学习·工作 / 健身 / 三餐均配置 `FollowUp`；归家时间与备注卡无 Tag 二级。
- 主选变更导致二级失效时，由 `PlanFlowReducer` 自动清除 `subSelection`。

### 4.6 HourTimePicker

- 时间数字 `40sp / ExtraBold`，`AnimatedContent` 上滑切换。
- `Slider` 保留小时吸附 `steps`；起止刻度 `labelSmall`。

### 4.7 PlanResultContent

- 成就区：圆形 `primary` 底 + `Check` 图标 +「今日规划完成 🎉」。
- 摘要：`surface` 圆角卡内可滚动 `bodyLarge`。
- `ConfettiBurst` 覆盖最上层，不拦截点击。
- 底部：`ChunkyButton(Primary)` 复制 + `ChunkyButton(Outline)` 返回首页。

---

## 5. 一天规划屏布局 — `PlanContent`

```
Column (fillMaxSize, padding top/bottom)
├── PlanProgressBar          // 水平 screenPadding
├── PlanCardFrame (weight 1)
│   └── AnimatedContent(contentKey = currentIndex)
│       └── CardBody         // 必须用 lambda 参数 state 快照
└── ChunkyButton             // 「下一张」/「完成」
```

**CardBody 分发**（`PlanInteraction`）：

| interaction | UI 组件 |
|---|---|
| `MULTI_TAG` / `SINGLE_TAG` | `TagCard` |
| `HOUR_TIME` | `HourTimePicker` |
| `NOTE` | `OtherNoteCard` |

---

## 6. 数据与状态边界

| 层级 | 持有内容 | 不持有 |
|---|---|---|
| `PlanFlowState`（domain） | `currentIndex`、`answers` | 动画中间态 |
| `PlanUiState`（libui） | 当前卡定义、答案、进度标志 | follow-up 展开态（由答案推导） |
| Compose 局部 | `remember`、`*AsState` 动效 | 业务答案 |
| `PlanAction` | `ToggleTag` / `SelectSingle` / `SelectFollowUp` / `Next` / `Previous` / `Skip` / 时间备注 | — |

单向数据流：`PlanContent(state, onAction)` ← `PlanScreen` ← `PlanViewModel`。

---

## 7. 扩展指南

### 7.1 新增卡片

1. `PlanCardType` 增加枚举值  
2. `PlanCardCatalog.cards` 追加 `PlanCardDefinition`  
3. 若需二级：配置 `followUp = FollowUp(title, options, triggers, skip)`  
4. `PlanTextExporter` 补充导出格式  
5. 无需改 UI，除非新 `PlanInteraction` 类型

### 7.2 FollowUp 配置语义

```kotlin
data class FollowUp(
  val title: String,              // 二级区标题，如「怎么做」「强度」
  val options: List<String>,
  val triggers: Set<String> = emptySet(),  // 空 = 任意主选触发
  val skip: Set<String> = emptySet(),      // 命中则不展示二级
)
```

| 场景 | triggers | skip |
|---|---|---|
| 三餐·自己做 | `{"自己做"}` | — |
| 健身·强度 | —（空=任意时段） | `{"今日练休"}` |

答案存储：`PlanCardAnswer.selectedOptions`（主选）+ `subSelection`（二级单选）。

### 7.3 库存链路复用

盘点流可直接复用 `ChunkyButton` / `ChunkyChip` / `TactileSurface` / `Dimens` / `Motion`；进度与卡框按物品数量适配 `PlanProgressBar` 的 `totalCount` 参数。

---

## 8. 依赖

- Compose BOM 内：`animation`、`foundation`、`material3`
- `material-icons-extended`（Rounded 图标，R8 按引用裁剪）
- **禁止**为视觉引入额外三方 UI 库

---

## 9. 验收清单（改 UI 时自检）

- [ ] 间距/圆角/动效均来自 `Dimens` / `Motion`，无魔法数字
- [ ] `PlanCardFrame` 内层 `fillMaxSize`，无灰底露出
- [ ] `AnimatedContent` 使用 `contentKey` + lambda 内 state 快照
- [ ] follow-up 用卡内 `AnimatedVisibility`，不进换卡 key
- [ ] chip 无 scale 弹簧；换卡同时运行动画层 ≤ 2
- [ ] 暗色模式下 `depth_*` 对比可读
- [ ] `./gradlew :libui:assembleDebug` 通过

---

## 10. 变更记录

| 版本 | 日期 | 变更 |
|---|---|---|
| v1.0 | — | 初版 Chunky 改皮方案 |
| v1.1 | 2026-06 | 对齐实现：M3 8dp 栅格；分段进度；卡面填满；AnimatedContent 快照修复；通用 FollowUp 二级选单；移除健身子步骤横滑与 chip scale 弹簧 |
| v1.2 | 2026-06 | 出门/工作补 FollowUp；Chip 双列栅格 + 未选描边；二级标题 `SectionTitleStyle`；对勾 fade 无横向展开 |
