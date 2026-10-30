# TodoDemo 项目级 AGENTS.md

本文件承载项目事实；编码规范以用户全局 AGENTS.md 为准，冲突时用户当前对话指令 > 本文件 > 全局。

## 模块地图

- 单模块 `:app`（Android application，Compose 单界面）。
- 包结构：`domain/model`、`domain/repository`（冻结契约，见 `docs/prd/todo-points-rank.md` 第 2 节）、`domain`（域引擎，issue 1）、`data`（Room 数据层，issue 2）、`ui`（ViewModel + Compose，issue 3）。

## 构建与验证任务（最小任务，worker 必须用这两个）

```bash
./gradlew --offline :app:compileDebugKotlin
./gradlew --offline :app:testDebugUnitTest
```

本仓库无 config 维度模块，统一用 `Debug` 任务名，不存在 `InternalDebug` 变体。

## 环境事实

- minSdk 24 / compileSdk 37 / targetSdk 37；Java 17 target（daemon 跑 Android Studio JBR）。
- Gradle 9.6.0 wrapper；`org.gradle.java.installations.auto-download=false` 已固化在 gradle.properties。
- 版本目录 `gradle/libs.versions.toml`：AGP 9.4.1（内置 Kotlin）、compose-compiler 2.4.20、KSP 2.3.12、Room 2.8.5。**依赖清单冻结，issue 不得增删依赖。**
- 骨架首次构建已联网拉齐全部依赖缓存；此后一切 Gradle 命令 `--offline`。
- git push / gh 如需代理：`127.0.0.1:7897`。

## 契约清单（红线 18）

无对外契约（无埋点/RC/广告/推送/deep link）。**对内契约 = `docs/prd/todo-points-rank.md` 第 2 节冻结接口**（骨架已提交的 `domain/model/*`、`domain/repository/TodoRepository.kt` 任何 issue 不得改动；PRD 冻结的新建签名不得偏离）。

## 红线豁免登记（随 multi-agent-pr-flow skill，2026-10-30 用户授权）

红线 15（git 操作）、红线 16（ADB/模拟器/UI 自动化，仅限本仓库应用包）在本 skill 执行期间放开，范围仅限本仓库及其 worktree。详见 DECISIONS.md。

## worktree

父目录 `E:\work\TodoMutilAgentWorkFlowDemo\wt\`；worker 树 `wt-issue-<N>`，reviewer 树 `wt-review-<P>`。
