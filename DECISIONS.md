# DECISIONS.md

按业务问题索引（不按文件）。变更前先查本表。

## 豁免登记（multi-agent-pr-flow skill 执行期）

- **日期**: 2026-10-30
- **背景**: 用户授权在本仓库执行 multi-agent-pr-flow skill，skill 需要子 agent 执行 git/gh 操作。
- **方案**: 红线 15（git 操作）、红线 16（ADB/模拟器/UI 自动化，仅限本仓库应用包）在 skill 执行期间放开，范围仅限本仓库及全部 worktree。
- **被否备选**: 无（skill 阶段 0 第 4 条的预授权条款）。

## TodoDemo 需求定稿（PRD frozen）

- **日期**: 2026-10-30
- **背景**: 单界面 Todo demo + 积分称号，目标是演练多 agent 工作流。
- **方案**: 用户全量批准第 1 轮提案——Room 持久化；每完成一个 +10 分；完成后锁定不可取消、不可删除；未完成可删；5 档称号阈值 0/30/80/150/300（新手/见习/勤奋/达人/传奇）；积分称号常驻界面顶部。
- **被否备选**: 可取消完成+扣分降档（逻辑翻倍，demo 不值）；内存态不持久化（数据层无内容可测）。

## 骨架先行 + 两波 fan-out

- **日期**: 2026-10-30
- **背景**: 空仓直接 fan-out 则 worker 分支无 Gradle 骨架编译不了；issue 3（UI）编译依赖 issue 1（RankResolver）与 issue 2（TodoRepositoryImpl）的产物，三者并行会导致 worker 3 编译失败。
- **方案**: 主 agent 先把骨架（构建配置 + 冻结接口 + AGENTS.md + 本文件 + PRD）提交 main；wave 1 = issue 1+2 并行（只依赖骨架类型），wave 2 = issue 3 在 1+2 合并后派发。合并顺序 1→2→3。
- **被否备选**: 三 issue 全并行（issue 3 必然 blocked(dependency-wait)；用骨架放 RankResolver 桩又会掏空 issue 1）。

## 版本选型（最大化本机缓存命中）

- **日期**: 2026-10-30
- **背景**: 全新项目本机无缓存；本机已有 AGP 9.4.1 / Kotlin 2.4.20 / Gradle 9.6.0 / Room 2.8.5 / lifecycle 2.11.0 缓存。
- **方案**: 照 FindView 项目选 AGP 9.4.1（内置 Kotlin）+ compose-compiler 2.4.20 + Gradle 9.6.0；Room 2.8.5 + KSP 2.3.12；compose ui/material3 用显式版本（ui 1.12.1 / material3 1.4.0）。骨架构建联网一次拉齐全部缓存，此后全程 `--offline`（已验证可行）。
- **被否备选**: 自选最新稳定版（离线不可控）；compose BOM + platform()（本机 Gradle 9.6 下约束未生效，ui 版本解析为空，改显式版本）。另注意坐标陷阱：ui 的 group 是 `androidx.compose.ui` 不是 `androidx.compose`。

## 仓库可见性

- **日期**: 2026-10-30
- **背景**: skill 红线 19 要求仓库私有；本仓创建为 PUBLIC。
- **方案**: demo 无用户数据，PRD/issue/PR 内容已约束不携带用户数据，先按 PUBLIC 继续；已向用户提示，是否转私有由用户定。
- **被否备选**: 未经确认直接 `gh repo edit --visibility private`（外向动作，不代拍）。
