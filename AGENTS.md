# LifePlanner 项目级规则

本规则适用于整个仓库。目标是在不改变既有功能、数据兼容性和用户链路的前提下，保持代码现代、清洁、简洁。

## 事实与边界

- 架构、产品范围和模块职责冲突时，以根目录 `README.md` 为准。
- 模块固定为 `:app`、`:libui`、`:core:domain`、`:core:database`、`:feature:todo|schedule|diary|dishes|inventory`。
- `:app` 只负责 Application、依赖组装、导航和顶层页面结构。
- `:libui` 只放主题、token 和无业务状态的通用 Compose 组件。
- `:core:domain` 只放领域模型、纯规则和 Repository 接口。
- `:core:database` 只放 Room、DAO、Entity、Mapper 和 Repository 实现。
- Feature 之间不得直接依赖；Feature 不得访问 DAO 或 Entity。
- Room schema、Migration、路由参数、Repository 契约和持久化字段属于兼容链路，不得作为“冗余”删除或随意改名。

## 精简原则

- 只实施能够证明行为等价的精简；功能、状态流、导航、数据库和交互语义优先于代码行数。
- 优先消除同一事实的重复定义、无意义中间变量、重复分支、重复映射和每次重组产生的静态对象。
- 不为抽象而抽象。跨 Feature 的相似 UI 如果业务语义不同，应保留在各自 Feature，不得为了去重破坏模块边界。
- 不新增兼容层、历史别名、空壳模块、无调用接口或仅有一个调用方且不能降低复杂度的包装层。
- 不因清理代码而升级依赖、引入第三方库、改变公开 API 或扩大改动范围。
- 页面尺寸、颜色、形状和动效继续使用 `:libui` 的 token，不新增页面级裸值。
- 修改前先检查 `git status` 和真实调用点；保留用户已有改动，不格式化无关文件。

## 测试与验证

- 本项目不维护 `src/test`、`src/androidTest`、测试 Runner 或测试专用依赖；除非用户明确要求，不得重新引入。
- 默认质量门禁只有静态检查：

  ```powershell
  .\gradlew.bat lintDebug --console=plain
  ```

- 同时执行 `git diff --check` 和失效引用扫描；不得自动运行 unit test、instrumentation、connected test 或 assemble。
- `lintDebug` 失败时修复根因，不得关闭规则、降级检查或删除业务逻辑换取通过。
- 交付时明确说明 lint 结果、剩余 warning，以及运行时/真机链路未验证的边界。

## 交付要求

- 说明改动涉及的模块、被合并或删除的重复点，以及代码量变化。
- 若某个候选精简无法百分之百确认行为等价，只报告，不修改。
- 不提交、不 push，除非用户明确授权。
