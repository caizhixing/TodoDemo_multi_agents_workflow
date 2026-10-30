# PRD：Todo 积分称号 Demo

- **milestone**: todo-points-rank
- **日期 / 对谈轮次**: 2026-10-30 / 2（第 1 轮提案，第 2 轮用户全量批准）
- **状态**: frozen（用户 2026-10-30 批复「都可以，你执行吧」）

## 1. 背景与目标

用最小可用的 Todo 应用完整演练 multi-agent-pr-flow 多 agent PR 工作流。

用户看到：单个界面的 Todo 列表，可添加 todo、勾选完成、删除未完成的 todo；顶部常驻积分与当前称号。
用户拿到：每完成一个 todo 得 10 积分；积分累积决定称号档位（5 档），称号只升不降（积分由完成数推导，完成不可撤销，故无降档路径）。
失败时：删除已完成的 todo 被拒绝（按钮不可用即不触发）；重复完成同一 todo 不重复发分（返回 AlreadyCompleted，不崩溃、不加分）。
成功标准：三个 issue 全部通过跨模型评审、按序合并、集成门禁（整体编译 + 全量单测）绿。

## 2. 冻结接口与数据模型（已随骨架提交 main，worker 不得修改）

骨架包 `com.example.tododemo`，单模块 `:app`。

**已提交的类型（文件已在 main，任何 issue 不得改动）：**

```kotlin
// domain/model/TodoItem.kt
data class TodoItem(id: Long, title: String, isCompleted: Boolean, completedAtMillis: Long? = null)

// domain/model/RankTier.kt —— 裸枚举，阈值与显示名归 issue 1 的 RankResolver
enum class RankTier { NOVICE, APPRENTICE, DILIGENT, EXPERT, LEGEND }

// domain/model/PlayerProgress.kt
data class PlayerProgress(totalPoints: Int, completedCount: Int, rank: RankTier)

// domain/repository/TodoRepository.kt
interface TodoRepository {
    fun observeTodos(): Flow<List<TodoItem>>
    suspend fun addTodo(title: String): Long
    suspend fun completeTodo(id: Long): CompleteOutcome
    suspend fun removeTodo(id: Long): Boolean   // true=已删除；false=不存在或已完成（锁定不可删）
}
sealed interface CompleteOutcome { Completed; AlreadyCompleted; NotFound }
```

**PRD 冻结、由 worker 新建的签名（worker 按此实现，不得自行改名/改参）：**

```kotlin
// issue 1 新建，包 com.example.tododemo.domain
object PointsPolicy { const val POINTS_PER_TODO: Int = 10 }
object RankResolver {
    fun resolveForPoints(points: Int): RankTier
    // 阈值：[0,30) NOVICE；[30,80) APPRENTICE；[80,150) DILIGENT；[150,300) EXPERT；[300,+∞) LEGEND
    fun displayName(tier: RankTier): String
    // 显示名：NOVICE=新手 / APPRENTICE=见习 / DILIGENT=勤奋 / EXPERT=达人 / LEGEND=传奇
}
object ProgressCalculator {
    fun calculate(todos: List<TodoItem>): PlayerProgress
    // totalPoints = 完成数 × POINTS_PER_TODO；completedCount = 完成数；rank = RankResolver.resolveForPoints(totalPoints)
}

// issue 3 新建，包 com.example.tododemo.ui
data class TodoListUiState(
    val items: List<TodoItem>,        // 未完成在前（创建时间新→旧），已完成在后（完成时间新→旧）
    val totalPoints: Int,
    val completedCount: Int,
    val rankTitle: String,            // RankResolver.displayName(当前档位)
    val isBusy: Boolean,              // 写操作进行中，防重复提交
)
class TodoListViewModel(repository: TodoRepository) : ViewModel() {
    val uiState: StateFlow<TodoListUiState>
    fun addTodo(title: String)        // 空白标题直接忽略
    fun completeTodo(id: Long)
    fun removeTodo(id: Long)
}
```

**行为约定（跨 issue 共同遵守）：**
- `completeTodo` 二次调用同一 id → `AlreadyCompleted`，不加分不崩溃；id 不存在 → `NotFound`。
- `removeTodo` 对已完成项返回 `false` 且数据不变。
- `addTodo` 对空白（trim 后为空）标题的行为由 VM 拦截（不产生 todo）；Repository 层不做校验，存什么插什么。
- `observeTodos()` 排序规则由 Repository 实现保证（见 issue 2 行为清单）。

## 3. 任务拆分预案与合并顺序

| 序号 | issue 主题 | 触碰模块 | 依赖 / 冲突提示 |
|---|---|---|---|
| 1 | 积分与称号域引擎（PointsPolicy + RankResolver + ProgressCalculator） | 新文件 `domain/` | 无依赖；与 2、3 不同文件 |
| 2 | 数据层（Room + TodoRepositoryImpl） | 新文件 `data/` | 无依赖；与 1、3 不同文件 |
| 3 | UI 层（TodoListViewModel + TodoListScreen + MainActivity 接线） | 新文件 `ui/` + 改 `MainActivity.kt` | **依赖 1、2 合并后才能开工**（编译需要 RankResolver 与 TodoRepositoryImpl）；wave 2 |

合并顺序 = 1 → 2 → 3。issue 1、2 为 wave 1 并行 fan-out（并发 2）；issue 3 在 1+2 合并进 main 后单独派发。

## 4. 逐任务行为清单（验收标准）

### 任务 1：积分与称号域引擎
- 行为 1.1：`POINTS_PER_TODO == 10`。
- 行为 1.2：`resolveForPoints` 边界——29→NOVICE，30→APPRENTICE，79→APPRENTICE，80→DILIGENT，149→DILIGENT，150→EXPERT，299→EXPERT，300→LEGEND，301→LEGEND，0→NOVICE。
- 行为 1.3：负数积分（防御路径）→ NOVICE，不抛异常。
- 行为 1.4：`displayName` 五档各返回 新手/见习/勤奋/达人/传奇。
- 行为 1.5：`calculate` 对混合列表（含未完成项）只统计完成项积分与完成数，且 rank 与 totalPoints 一致。
- 行为 1.6：`calculate` 空列表 → PlayerProgress(0, 0, NOVICE)。

### 任务 2：数据层
- 行为 2.1：`observeTodos()` 排序——未完成在前按创建时间新→旧，已完成在后按完成时间新→旧。
- 行为 2.2：`addTodo` 返回新 id，插入项 isCompleted=false、completedAtMillis=null。
- 行为 2.3：`completeTodo` 未完成 id → Completed，持久化 isCompleted=true 且 completedAtMillis>0。
- 行为 2.4：`completeTodo` 已完成 id → AlreadyCompleted，数据不变。
- 行为 2.5：`completeTodo` 不存在 id → NotFound。
- 行为 2.6：`removeTodo` 未完成 id → true 且项消失。
- 行为 2.7：`removeTodo` 已完成 id → false 且项保留。
- 行为 2.8：`observeTodos` 在增删改后发出新列表（响应式）。
- 行为 2.9：Room schema 与实体映射——entity 字段与 TodoItem 一一对应（id 自增主键）。

### 任务 3：UI 层
- 行为 3.1：初始状态 items 空、积分 0、称号「新手」。
- 行为 3.2：`addTodo("买牛奶")` 后 items 出现该项且位于未完成区首位。
- 行为 3.3：空白标题（含纯空格）`addTodo` 后 items 不变。
- 行为 3.4：`completeTodo(id)` 后该项移入已完成区，totalPoints +10，completedCount +1。
- 行为 3.5：跨档位——完成 3 个后 rankTitle 变为「见习」。
- 行为 3.6：`removeTodo(未完成id)` 后该项消失且积分不变。
- 行为 3.7：UI 事件映射——Screen 的输入框/添加按钮/勾选/删除回调正确调用 VM 对应方法（Compose UI 不设覆盖数字，靠编译 + 评审）。

## 5. 测试策略

| 任务 | 层级 | 覆盖目标 | 特殊测试点 |
|---|---|---|---|
| 1 | 纯域逻辑 | 100% 行覆盖 | 阈值边界值（29/30/79/80/149/150/299/300/301）、负数防御、空列表 |
| 2 | Repository（fake DAO） | ≥90% | 排序稳定性、锁定删除拒绝、响应式发射、二次完成幂等 |
| 3 | ViewModel（fake repository） | ≥90% | 空白标题拦截、跨档位积分、isBusy 防重；Compose Screen 不设数字 |

测试栈固定：JUnit4 + Truth + kotlinx-coroutines-test（已在骨架依赖）。禁止新增任何依赖。fake 优先于 mock；禁止 mock 被测对象本身。

## 6. PRD 外决策清单（实现中发现再记，格式：决策 | 理由 | 代码位置）

- （空）

## 7. 契约影响面（红线 18 核对项）

无对外契约：本 demo 无埋点、无 Remote Config、无广告、无推送、无对外 Intent action/extra/deep link。**本次 fan-out 的「契约」即第 2 节冻结接口**——worker 改冻结接口等同破坏契约，reviewer 按 contract_touched=yes 处理。

## 8. 状态协议

默认按 skill references/status-protocol.md 执行，无额外里程碑。
特记：issue 3 不参与 wave 1 fan-out；主 agent 在 1+2 合并后派发，派发前将 issue 3 label 保持 status/todo。
