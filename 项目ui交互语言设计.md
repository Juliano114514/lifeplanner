# LifePlanner UI 交互语言设计规范

> 设计语言：Calm Playful（克制活力）
>
> 平台基础：Jetpack Compose + Material 3
> 设计系统模块：`:libui`

## 1. 设计定位

LifePlanner 是高频使用的个人工具。界面需要有亲和力，但不能让装饰、阴影和动画干扰任务、时间与库存信息。

因此采用以下组合：

- **Material 3 负责结构**：语义色、字体阶梯、形状、导航和可访问性。
- **国内设计体系负责秩序**：组件分级、设计 token、统一状态和高信息密度下的清晰度。
- **多邻国负责情绪**：圆润、明亮、直接反馈，但只用于高价值操作。
- **LifePlanner 自己负责克制**：3D 厚度只用于主按钮；卡片、导航、表单不重复堆叠强阴影。

这不是对任一产品的复刻，而是针对本项目使用场景的统一规则。

## 2. 调研依据

| 来源 | 采用内容 | 本项目取舍 |
|---|---|---|
| [Google Material 3 for Compose](https://developer.android.com/develop/ui/compose/designsystems/material3) | 颜色、字体、形状、组件强调层级 | 作为 Android 结构基线 |
| [Android 触控目标规范](https://support.google.com/accessibility/android/answer/7101858) | 48dp 最小触控目标与控件间距 | 所有可点击控件不低于 48dp |
| [字节 Arco Design Token](https://arco.design/react/docs/token) | 语义 token、文字与容器层级 | 禁止页面散落颜色和尺寸 |
| [字节 Arco Button](https://arco.design/react/components/button) | 按钮类型、尺寸和强调级别 | 统一为五种语义按钮 |
| [腾讯 TDesign Button](https://tdesign.tencent.com/qq-miniprogram/components/button) | 按钮尺寸、状态和反馈的一致性 | 统一 enabled/pressed/loading/disabled |
| [快手小程序设计指南](https://open.kuaishou.com/docs/design/designPrinciples/guide) | 语言一致、重点突出、反馈及时 | 一屏一个主操作，状态及时可见 |
| [Duolingo 视觉风格](https://blog.duolingo.com/shape-language-duolingos-art-style/) | 明亮色彩、圆角按钮、少量高价值动画 | 保留亲和感，限制动效数量 |
| [Duolingo Typography](https://design.duolingo.com/identity/typography) | 短标题使用更强字重，正文保持可读 | 系统字体 + 明确字重，不引入品牌字体 |

## 3. 核心原则

1. **一屏一个主操作**：同一视觉区域最多出现一个 Primary 按钮。
2. **层级先靠色调和间距**：卡片默认不用硬阴影；不能用阴影解决所有分组问题。
3. **尺寸来自 token**：页面禁止新增裸 `dp`、颜色值和动画时长。
4. **状态不只靠颜色**：逾期、冲突、临期、低库存必须同时显示文字。
5. **反馈即时且短促**：按压 100ms，普通变化 180ms，强调变化不超过 300ms。
6. **可访问性优先**：触控目标至少 48dp，保留系统字体缩放，图标按钮提供描述。

## 4. Token

### 4.1 间距

基于 4dp 网格，定义于 `theme/Tokens.kt`：

| Token | 值 | 使用场景 |
|---|---:|---|
| `AppSpacing.xxs` | 2dp | 极小视觉修正 |
| `AppSpacing.xs` | 4dp | 图标与紧邻元素 |
| `AppSpacing.sm` | 8dp | 同组控件、Chip 间距 |
| `AppSpacing.md` | 12dp | 卡片内部小节、表单项 |
| `AppSpacing.lg` | 16dp | 页面水平边距、卡片内容边距 |
| `AppSpacing.xl` | 24dp | 页面大区块 |
| `AppSpacing.xxl` | 32dp | 空状态与大留白 |

约束：

- 手机页面默认水平边距 16dp。
- 同组元素间距 8–12dp。
- 跨区块间距 24dp。
- 不使用 6、10、14、20 等独立间距值。

### 4.2 尺寸

| Token | 值 | 说明 |
|---|---:|---|
| `touchTarget` | 48dp | 最小触控目标 |
| `button` | 48dp | 默认按钮面高度 |
| `buttonLarge` | 56dp | 页面底部主操作 |
| `iconSmall` | 18dp | 按钮、Chip 内图标 |
| `icon` | 24dp | 常规图标 |
| `fab` | 56dp | 浮动操作按钮 |
| `progress` | 8dp | 向导进度条 |
| `calendarCell` | 48dp | 日历触控单元 |

Primary 和 Danger 按钮另有 4dp 底部厚度，总布局高度分别为 52dp 或 60dp。

`AppFab` 的外尺寸固定为 56dp。内部使用纵向布局同时展示 18dp 图标和单行短标签，不通过扩大 FAB 或页面级尺寸适配文字。

### 4.3 圆角

圆角通过 `MaterialTheme.shapes` 获取：

| Shape | 圆角 | 使用 |
|---|---:|---|
| `extraSmall` | 8dp | 小标签、紧凑容器 |
| `small` | 12dp | 输入框、小控件 |
| `medium` | 16dp | 按钮、Chip |
| `large` | 20dp | 普通卡片 |
| `extraLarge` | 28dp | Dialog、大容器、空状态图标底 |

禁止在页面中直接创建 `RoundedCornerShape`。

### 4.4 描边与深度

- 普通描边：1dp。
- 选中描边：2dp。
- 卡片：1dp 低对比边框，无硬阴影。
- Primary / Danger 按钮：4dp 同色深阶底面。
- Secondary / Outline / Text：无 3D 深度。

## 5. 颜色

颜色定义于 `theme/Color.kt`，页面只使用 `MaterialTheme.colorScheme` 或 `LifePlannerDesign.colors`。

### 5.1 品牌与语义

| 角色 | 浅色主题 | 主要用途 |
|---|---|---|
| Primary | `#167A45` | 主操作、选中态、焦点 |
| Primary Container | `#B7F4CF` | 选中 Chip、轻强调容器 |
| Secondary | `#3B6473` | 次级信息与次操作 |
| Tertiary | `#805600` | 警告与时间提醒 |
| Error | `#BA1A1A` | 错误、逾期、冲突 |
| Background | `#F7F9F7` | 页面背景 |
| Surface | `#FFFFFF` | 卡片与输入容器 |

暗色主题使用独立色阶，不通过透明黑白覆盖浅色主题。

### 5.2 使用规则

- Primary 只用于主操作与明确选中态。
- Success、Warning、Error 使用对应 container/on-container 组合。
- 正文使用 `onSurface`；辅助信息使用 `onSurfaceVariant`。
- 禁用态由组件统一计算，页面不自行设置透明度。
- 不在 Feature 中写十六进制颜色。

## 6. 字体层级

字体定义于 `theme/Type.kt`，使用系统字体：

| 样式 | 字号/行高 | 字重 | 使用 |
|---|---|---|---|
| `displaySmall` | 36/44sp | ExtraBold | 大数值、快速向导时间 |
| `headlineLarge` | 32/40sp | Bold | 特殊首页标题 |
| `headlineMedium` | 28/36sp | Bold | 向导问题 |
| `headlineSmall` | 24/32sp | Bold | 空状态标题 |
| `titleLarge` | 22/28sp | Bold | 顶部栏、页面标题 |
| `titleMedium` | 16/24sp | SemiBold | 卡片标题、区块标题 |
| `titleSmall` | 14/20sp | SemiBold | 小节标题 |
| `bodyLarge` | 16/24sp | Regular | 主要正文 |
| `bodyMedium` | 14/20sp | Regular | 默认正文 |
| `bodySmall` | 12/16sp | Regular | 辅助说明 |
| `labelLarge` | 14/20sp | Bold | 按钮、Chip |
| `labelMedium` | 12/16sp | SemiBold | Badge、计数 |

同一张卡片最多使用三种文字层级。

## 7. 按钮规范

统一组件：`AppButton`。

| Variant | 强调级别 | 视觉 | 典型动作 |
|---|---|---|---|
| `Primary` | 最高 | 绿色实心 + 4dp 厚度 | 保存、下一步、生成 |
| `Secondary` | 中高 | 次色浅底，无厚度 | 快速向导、补充操作 |
| `Outline` | 中 | 透明底 + 1dp 描边 | 返回、替代路径 |
| `Text` | 低 | 无底无描边 | 跳过、取消、查看详情 |
| `Danger` | 高风险 | 错误色实心 + 4dp 厚度 | 确认删除、不可逆动作 |

尺寸：

- `Medium`：48dp，常规操作。
- `Large`：56dp，页面底部主操作。
- 图标 18dp，图标与文字间距 8dp。
- 最小宽度 48dp；全宽由调用方显式 `fillMaxWidth()`。

状态：

- **Pressed**：主按钮面下沉 4dp，100ms。
- **Loading**：图标槽显示进度，禁用重复点击。
- **Disabled**：使用 surface variant 和低强调文字。
- **Focus/Accessibility**：由 Compose clickable/Surface 语义提供。

## 8. 容器与选择组件

### 8.1 AppCard

| Style | 用途 |
|---|---|
| `Default` | 普通内容、列表项 |
| `Tonal` | 日历、说明区等弱分组 |
| `Selected` | 当前选中卡片 |

规则：

- 默认 20dp 圆角、1dp 边框、16dp 内容边距。
- 可点击卡片使用 Material ripple，不使用 3D 深度。
- 卡片内部不再额外重复 `padding(16.dp)`。

### 8.2 AppChoiceChip

- 高度不低于 48dp。
- 未选：Surface + 1dp 弱描边。
- 选中：Primary Container + 2dp Primary 描边 + Check。
- 单选和多选共用同一视觉，只由业务层决定选择逻辑。

### 8.3 AppStatusBadge

提供 Neutral、Success、Warning、Error 四种语义。Badge 只表达状态，不承担点击操作。

### 8.4 AppDateNavigator

- 视口固定显示五天，每项由月份文字和 48dp 正圆日期块组成，并允许用户自由横向滑动。
- 日期块内显示日与周几，下方保留记录圆点槽；未选中日期有记录时显示 Primary 圆点，无记录时显示灰色圆点。
- 今天在未选中时使用与日期块同尺寸的灰色 3dp 正圆描边；选中项使用 Primary，并隐藏记录圆点和今天描边，同时通过字重和无障碍语义区分。
- 初始数据窗口为选中日前后各 10 天；只有滑动触达窗口边界时才按当前方向扩展下一组 10 天。
- 只有选中日期发生变化时才让该日期轻量滚动到视口中央；“回今天”是显式居中操作，即使今天已选中也会重新居中；普通滑动、记录状态变化和数据窗口扩展不得触发自动居中。
- 五个等宽日期槽共同覆盖导航 Bar；长按任意位置打开日期选择器，保留远日期跳转能力。
- Bar 左下角提供“回今天”Text 按钮，视觉保持紧凑，触控目标仍不低于 48dp。

### 8.5 紧凑日记条目

- 开心与不开心条目使用区块标题、紧凑行和分隔线，不为每条记录重复添加卡片边框。
- 条目不使用 Todo 勾选框；普通点击不触发编辑，长按进入预览、修改或删除。
- 完整日记正文可以使用一张 Tonal 卡片展示，编辑页使用单个 `LazyColumn`，禁止嵌套可滚动列表。
- 编辑页“保存日记”在同一事务中保存未添加的非空条目草稿和完整正文，全部成功后返回；失败时保留输入并显示错误。

## 9. 页面层级

标准页面从上到下：

1. `AppTopBar`：22sp 标题；二级页面统一返回图标。
2. 页面内容：水平 16dp；区块间 24dp。
3. 列表：卡片间 12dp；区块标题带数量。
4. 主操作：FAB 或底部 Primary 按钮，二者按场景选一。
5. 底部导航：任务、日程、日记、菜品、库存五个一级入口，选中项使用 Primary Container。

层级优先级：

```text
Primary action
  > 当前任务/选中内容
  > 页面标题
  > 卡片标题
  > 正文
  > 辅助信息
```

## 10. 反馈与动效

| Token | 时长 | 使用 |
|---|---:|---|
| `fast` | 100ms | 按压、细微状态 |
| `standard` | 180ms | 选中、展开、内容切换 |
| `emphasized` | 300ms | 页面级或完成反馈 |

规则：

- 动画必须说明状态变化，不能只是持续装饰。
- 同一时刻最多一个高注意力动画。
- 列表滚动不叠加卡片逐个入场。
- 加载、空状态、错误统一使用 `AppLoadingState`、`AppEmptyState`、`AppErrorState`。
- 系统关闭动画时，Compose 动画应自然降级。

## 11. 架构边界

`:libui` 只包含：

```text
libui/
└── src/main/java/com/example/libui/
    ├── theme/
    │   ├── Color.kt
    │   ├── Shape.kt
    │   ├── Theme.kt
    │   ├── Tokens.kt
    │   └── Type.kt
    └── components/
        ├── AppButton.kt
        ├── AppCard.kt
        ├── AppChoiceChip.kt
        ├── AppChrome.kt
        ├── AppDateNavigator.kt
        └── AppFeedback.kt
```

禁止放入：

- ViewModel、Repository、DAO、Room Entity。
- Feature 专属 UiState、UiAction 和领域模型。
- Navigation Controller 或具体 Route。
- 页面级业务流程。

## 12. 开发检查

- [ ] 页面没有新增裸 `dp`、颜色值或动画时长。
- [ ] 一屏不超过一个 Primary 主操作。
- [ ] 可点击控件触控目标不低于 48dp。
- [ ] 状态同时有文字或图标，不只依赖颜色。
- [ ] 卡片没有重复内容 padding 或无意义硬阴影。
- [ ] 顶部栏、FAB、空/错/加载状态复用 `libui`。
- [ ] Feature 没有依赖其他 Feature。
- [ ] Light/Dark Theme 都能辨认文字与状态。
