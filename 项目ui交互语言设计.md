# 项目 UI 交互语言设计（v1.0 · 一天规划链路）

> 基调：**多邻国式 3D 厚按钮（Chunky / Neo-tactile）**
> 适用范围：首页入口 → 一天规划卡片流（8 张）→ 规划结果页
> 约束：仅调整视觉 / 布局 / 交互 / 动画，**不改动核心链路逻辑**；改动收敛在 `libui` 模块；系统字体 + 字重，零新增三方依赖与字体资源。

---

## 1. 设计原则（Why）

| 原则 | 含义 | 落地手段 |
|---|---|---|
| **可触摸的厚度** | 每个可点元素都有「实体感」——能按下去 | 底部硬阴影（offset shadow），按下时阴影收起 + 主体下沉 |
| **即时正反馈** | 每次操作都有 Q 弹响应，降低决策疲劳 | 选中弹跳放大、勾出现、完成撒花 |
| **大字少色** | 信息层级靠字重/字号，不靠堆颜色 | 仅 1 个品牌色 + 中性灰阶；标题 800 字重 |
| **一次一焦点** | 卡片流居中单卡，干扰最小 | 大留白 + 卡片切换平移动画 |
| **手势可回退** | 误操作零成本 | 下滑回退 + 顶部「上一张」图标按钮 |

> 视觉血统：多邻国（厚按钮 + 撒花 + 弹跳）× 不背单词（克制留白）。品牌色 `#00DC5A` 亮绿天然贴合。

---

## 2. 设计 Token（唯一数据源）

集中定义，组件只引用 token，不写魔法数字，便于全局调参与维护。

### 2.1 圆角 / 间距 / 厚度 —— `theme/Dimens.kt`（新增）

```kotlin
package com.example.libui.theme

import androidx.compose.ui.unit.dp

/** 设计系统的尺寸与节奏 token（圆角 / 厚度 / 动效），组件统一引用。 */
object Dimens {
  // 圆角阶梯
  val radiusChip = 16.dp      // 胶囊 chip
  val radiusButton = 18.dp    // 厚按钮
  val radiusCard = 28.dp      // 卡面（大圆角更"软萌"）

  // 3D 厚度：主体与底部硬阴影的垂直偏移
  val depthButton = 4.dp      // 按钮下沉行程
  val depthCard = 6.dp        // 卡片底影
  val depthChip = 3.dp        // chip 选中时的底影

  // 节奏
  val gapTag = 10.dp
  val cardPadding = 24.dp
  val screenPadding = 20.dp
}

/** 动效时长与曲线，保证全链路一致。 */
object Motion {
  const val FAST = 120        // 按压反馈
  const val MEDIUM = 260      // 卡片切换 / chip 弹跳
  const val SLOW = 420        // 撒花 / 进度推进
}
```

### 2.2 硬阴影底色 —— `res/values/colors.xml` 追加

「厚度」来自主体色下方压着一层更深的同色系底块。

```xml
<!-- 3D 厚按钮底部硬阴影（比主体深一阶，非黑色投影） -->
<color name="depth_primary">#FF00A848</color>   <!-- 主按钮底：品牌绿压深 -->
<color name="depth_surface">#1F000000</color>   <!-- 中性元素底：低透明黑 -->
<color name="depth_card">#14000000</color>      <!-- 卡片底影 -->
```

`values-night/colors.xml` 追加：

```xml
<color name="depth_primary">#FF007A35</color>
<color name="depth_surface">#33000000</color>
<color name="depth_card">#40000000</color>
```

### 2.3 字体层级 —— `theme/Type.kt`（修改）

系统字体，靠字重拉开层级（多邻国辨识度的廉价平替）。

| 角色 | style | size / weight | 用途 |
|---|---|---|---|
| 屏标题 | `headlineMedium` | 28sp / **800** | 首页"生活规划"、结果页标题 |
| 卡片问题 | `headlineSmall` | 24sp / **700** | "出门""健身·强度" |
| 按钮文字 | `titleMedium` | 17sp / **700** | 厚按钮内文字 |
| 选项 chip | `labelLarge` | 16sp / 600 | tag 文本 |
| 辅助说明 | `bodyMedium` | 14sp / 500 | 副标题、提示 |
| 进度数字 | `labelLarge` | 15sp / **800** | "2 / 8" |

---

## 3. 核心组件：ChunkyButton（新增）

整套语言的地基。底部硬阴影制造厚度，按下时主体下移、阴影收起 = 物理下沉。

**路径**：`libui/src/main/java/com/example/libui/components/ChunkyButton.kt`

**结构**：`Box`（底影层）叠 `Box`（主体层）；`interactionSource` 监听按下，`animateDpAsState` 驱动主体 `offsetY`（0 → depth）与底影高度（depth → 0）。

```kotlin
enum class ChunkyStyle { Primary, Outline }

@Composable
fun ChunkyButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ChunkyStyle = ChunkyStyle.Primary,
  enabled: Boolean = true,
)
```

**交互规格**：
- 静止：主体在上，下方露出 `depthButton`(4dp) 的深色底块。
- 按下（`pressed`）：主体 `translationY = +4dp`，底块高度 → 0，`Motion.FAST` 弹性曲线。
- 抬起：回弹（`spring(dampingRatio = 0.55)`）。
- 禁用：整体降饱和 + 去底影。
- `Primary` 用品牌绿 + `depth_primary`；`Outline` 用透明面 + 描边 + `depth_surface`。

> 全链路所有主操作（下一张 / 完成 / 复制 / 返回）统一走此组件。

---

## 4. 选项卡片：ChunkyChip + TagChoiceGroup（新增 + 改皮）

**路径**：`libui/src/main/java/com/example/libui/components/ChunkyChip.kt`

替换 Material `FilterChip`，胶囊形，选中态有厚度 + 弹跳 + 勾。

```kotlin
@Composable
fun ChunkyChip(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
)
```

**交互规格**：
- 未选：浅灰填充（`surfaceVariant`），无底影。
- 选中：品牌绿浅底 + 绿描边 + `depthChip` 底影；文字前出现 `✓`（`Icons.Rounded.Check`，渐显）。
- 切换瞬间：`scale` 1 → 1.08 → 1 弹跳（`spring`，`Motion.MEDIUM`）。
- 点按：`scale` 轻微 0.96 缩放反馈。

`TagChoiceGroup` 内部把 `FilterChip` 换成 `ChunkyChip`，对外 API（`options/selected/multiSelect/onToggle/onSelect`）**完全不变**，`FlowRow` 间距改用 `Dimens.gapTag`。

---

## 5. 卡片框架：PlanCardFrame（改皮）+ 换卡动画

### 5.1 卡面
- 圆角 `radiusCard`(28dp)，`surface` 填充，底部 `depthCard`(6dp) 硬阴影（非弥散投影，保持"厚纸板"感）。
- 顶栏：左「上一张」、右「跳过」改为**圆形图标按钮**（`Icons.Rounded.ArrowBack` / `Icons.Rounded.Redo`）+ 极小文字，更轻更现代；`canGoPrevious=false` 时左键淡出。
- 标题字重提到 700，留白加大。
- 保留现有**下滑手势回退**（`detectVerticalDragGestures`）。

### 5.2 换卡动画（在 `PlanContent` 层）
用 `AnimatedContent` 包裹 `CardBody`，以 `state.currentIndex` 为 key：

```kotlin
AnimatedContent(
  targetState = state.currentIndex,
  transitionSpec = {
    // 前进：右进左出；后退：左进右出（按索引方向判断）
    (slideInHorizontally { it / 3 } + fadeIn()) togetherWith
      (slideOutHorizontally { -it / 3 } + fadeOut())
  },
  label = "card",
) { _ -> CardBody(state, onAction) }
```

> 方向判断：记住上一帧 index，`targetState > initialState` 为前进。轻量平移（1/3 宽）+ 淡入，不喧宾夺主。

---

## 6. 归家时间：HourTimePicker（改皮）

- 大号时间数字 `28sp / 800`，居中（"18:00"），换值时 `AnimatedContent` 做数字上滑切换。
- `Slider` 换厚滑轨：加粗 track（8dp）、品牌绿已填充段、加大圆 thumb（带 `depthChip` 底影），强化"物理吸附"手感（保留原吸附 `steps` 逻辑）。
- 起止刻度文字保留，弱化为 `labelSmall`。

---

## 7. 进度条：PlanProgressBar（改皮）

- 线性条改**圆角分段**（8 段对应 8 张卡）或圆角粗条（`clip(radiusChip)`，高 10dp）。
- 进度推进用 `animateFloatAsState`(`Motion.SLOW`) 平滑过渡。
- "2 / 8" 数字字重 800，当前数字步进时轻微弹跳。

---

## 8. 结果页：PlanResultContent（改皮）+ 撒花

- 顶部成就区：圆形绿色徽标 + `Icons.Rounded.Check` + "今日规划完成 🎉"，进入时撒花。
- **撒花**：纯 Compose `Canvas` 自绘（约 30 个彩色小方块，`Animatable` 下落 + 旋转 + 渐隐，`LaunchedEffect` 触发一次）。**不引依赖**，单文件 `components/ConfettiBurst.kt`（新增，可选）。
- 摘要文本包进圆角 `surface` 卡（`radiusCard` + `depthCard`），可滚动区域不变。
- 底部："复制到剪贴板"= `ChunkyButton(Primary)`；"返回首页"= `ChunkyButton(Outline)`。
- 复制成功：按钮文字短暂切换为"已复制 ✓"（视觉反馈，Toast 仍由 ViewModel Effect 负责，不改链路）。

---

## 9. 首页：HomeContent（改皮）

- 两个 `EntryCard` 改为 3D 可按压卡（复用 `ChunkyButton` 的下沉机制，或独立 `ChunkyCard`）：大圆角 + 底影 + 按下下沉。
- 左侧加大号 emoji / 图标（📅 一天规划、📦 库存统计）增强辨识。
- 禁用态（库存"敬请期待"）降饱和 + 去底影，仍可见但明显不可点。

---

## 10. 数据与状态

- **无任何状态变更**：所有动画均为 UI 局部 `remember` / `animate*AsState`，不进入 `UiState`。
- `PlanUiState` / `PlanAction` / `PlanResultUiState` / `HomeUiState` 字段与语义**保持原样**。
- 单向数据流不变：UI 仅消费 `state`、回调 `onAction`。

---

## 11. 依赖与配置

- 使用已在 BOM 内的 `androidx.compose.animation` / `foundation`（`Canvas`、`AnimatedContent`、`spring`）。
- **新增 `material-icons-extended`**（随 Compose BOM 统管版本，无需写版本号），用于顶栏图标按钮、勾、完成徽标等 Rounded 图标。R8/Proguard 会按实际引用裁剪，release 体积影响可控。

  `gradle/libs.versions.toml` `[libraries]` 追加：
  ```toml
  androidx-compose-material-icons-extended = { group = "androidx.compose.material", name = "material-icons-extended" }
  ```
  `libui/build.gradle.kts` `dependencies` 追加：
  ```kotlin
  implementation(libs.androidx.compose.material.icons.extended)
  ```
- 仅 `colors.xml` / `values-night` 各追加 3 个颜色；新增 `Dimens.kt` / `Motion`。

---

## 12. 可维护性与扩展

- 所有"厚度/圆角/动效"集中在 `Dimens` / `Motion`，调参单点生效。
- `ChunkyButton` / `ChunkyChip` / `ChunkyCard` 三件套可被后续「库存统计」链路直接复用，形成项目级设计系统。
- 组件对外 API 不变，纯内观替换，回滚 = 还原对应组件文件，互不牵连。

---

## 13. 风险与回滚

| 风险 | 说明 | 缓解 |
|---|---|---|
| 图标依赖 | `material-icons-extended` 体积大 | R8 按引用裁剪；仅 release 关注，debug 无影响 |
| 撒花性能 | 低端机粒子掉帧 | 限 30 粒、≤1.2s、一次性；可降级为静态 🎉 |
| 暗色对比 | 硬阴影底色在暗色下偏糊 | 已为 night 单独定底色，验收时校对 |
| 动画过度 | 喧宾夺主 | 平移幅度 1/3、时长 ≤260ms、曲线克制 |

**回滚点**：每个组件独立改皮，逐文件 `git checkout` 即可还原，无跨文件耦合。

---

## 14. 交付给 Coder 的检查清单

- [ ] 新增 `theme/Dimens.kt`（Dimens + Motion）
- [ ] `colors.xml` / `values-night/colors.xml` 各加 3 个 depth 色
- [ ] `Type.kt` 字重/字号层级
- [ ] `Theme.kt` 不破坏既有 `appColorScheme` 注入
- [ ] 新增 `components/ChunkyButton.kt`、`ChunkyChip.kt`（可选 `ChunkyCard.kt` / `ConfettiBurst.kt`）
- [ ] 改皮 `PlanCardFrame` / `TagChoiceGroup` / `HourTimePicker` / `PlanProgressBar`
- [ ] `PlanContent` 加 `AnimatedContent` 换卡 + 底部 `ChunkyButton`
- [ ] `PlanResultContent` / `HomeContent` / `PlanCardBodies` 换皮
- [ ] **契约校验**：所有 `*Content` 签名、`PlanAction`、`PlanUiState` 未变
- [ ] **依赖**：`libs.versions.toml` + `libui/build.gradle.kts` 加入 `material-icons-extended`
- [ ] **Lint 自检**：无未用 import、无新增未批准依赖（仅 icons-extended 已批准）、`./gradlew :libui:assembleDebug` 通过
```
