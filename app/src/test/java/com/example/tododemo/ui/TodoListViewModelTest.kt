package com.example.tododemo.ui

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoListViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val fake = FakeTodoRepository()
    private lateinit var viewModel: TodoListViewModel

    @Before
    fun setUp() {
        viewModel = TodoListViewModel(fake)
    }

    @Test
    fun initialState_isEmptyWithZeroProgressAndNoviceRank() = runTest {
        val state = viewModel.uiState.value

        assertThat(state.items).isEmpty()
        assertThat(state.totalPoints).isEqualTo(0)
        assertThat(state.completedCount).isEqualTo(0)
        assertThat(state.rankTitle).isEqualTo("新手")
        assertThat(state.isBusy).isFalse()
    }

    @Test
    fun addTodo_putsNewItemFirstInPendingSection() = runTest {
        viewModel.addTodo("写周报")
        viewModel.addTodo("买牛奶")

        val state = viewModel.uiState.value
        assertThat(state.items).hasSize(2)
        assertThat(state.items.first().title).isEqualTo("买牛奶")
        assertThat(state.items.first().isCompleted).isFalse()
        assertThat(state.totalPoints).isEqualTo(0)
    }

    @Test
    fun addTodo_withBlankTitle_isIgnoredWithoutTouchingRepository() = runTest {
        viewModel.addTodo("")
        viewModel.addTodo("   ")

        assertThat(viewModel.uiState.value.items).isEmpty()
        assertThat(fake.addTodoTitles).isEmpty()
    }

    @Test
    fun completeTodo_marksItemCompletedMovesItToTailAndAddsProgress() = runTest {
        viewModel.addTodo("买牛奶")
        viewModel.addTodo("写周报")

        viewModel.completeTodo(1L)

        val state = viewModel.uiState.value
        assertThat(state.items.map { it.id }).containsExactly(2L, 1L).inOrder()
        assertThat(state.items.last().isCompleted).isTrue()
        assertThat(state.totalPoints).isEqualTo(10)
        assertThat(state.completedCount).isEqualTo(1)
    }

    @Test
    fun completeTodo_threeItems_promotesRankTitleToApprentice() = runTest {
        repeat(3) { index -> viewModel.addTodo("任务$index") }

        viewModel.completeTodo(1L)
        assertThat(viewModel.uiState.value.rankTitle).isEqualTo("新手")

        viewModel.completeTodo(2L)
        viewModel.completeTodo(3L)

        val state = viewModel.uiState.value
        assertThat(state.rankTitle).isEqualTo("见习")
        assertThat(state.totalPoints).isEqualTo(30)
        assertThat(state.completedCount).isEqualTo(3)
    }

    @Test
    fun removeTodo_pendingId_removesItemAndKeepsPoints() = runTest {
        viewModel.addTodo("买牛奶")
        viewModel.addTodo("写周报")
        viewModel.completeTodo(1L)
        assertThat(viewModel.uiState.value.totalPoints).isEqualTo(10)

        viewModel.removeTodo(2L)

        val state = viewModel.uiState.value
        assertThat(state.items.map { it.id }).containsExactly(1L)
        assertThat(state.totalPoints).isEqualTo(10)
        assertThat(state.completedCount).isEqualTo(1)
    }

    @Test
    fun addTodo_whileBusy_ignoresSecondSubmitAndResetsBusy() = runTest {
        fake.addTodoGate = CompletableDeferred()
        viewModel.addTodo("买牛奶")

        assertThat(viewModel.uiState.value.isBusy).isTrue()
        viewModel.addTodo("写周报")

        fake.addTodoGate?.complete(Unit)
        advanceUntilIdle()

        assertThat(fake.addTodoTitles).containsExactly("买牛奶")
        val state = viewModel.uiState.value
        assertThat(state.isBusy).isFalse()
        assertThat(state.items.map { it.title }).containsExactly("买牛奶")
    }

    @Test
    fun addTodo_repositoryThrows_keepsLastValidStateAndResetsBusy() = runTest {
        viewModel.addTodo("买牛奶")

        fake.errorFactory = { RuntimeException("Room 故障") }
        viewModel.addTodo("写周报")

        val state = viewModel.uiState.value
        assertThat(state.isBusy).isFalse()
        assertThat(state.items.map { it.title }).containsExactly("买牛奶")
        assertThat(state.totalPoints).isEqualTo(0)

        fake.errorFactory = null
        viewModel.addTodo("重试")
        assertThat(viewModel.uiState.value.items.map { it.title }).containsExactly("重试", "买牛奶").inOrder()
    }

    @Test
    fun completeTodo_repositoryThrows_keepsLastValidStateAndResetsBusy() = runTest {
        viewModel.addTodo("买牛奶")

        fake.errorFactory = { RuntimeException("Room 故障") }
        viewModel.completeTodo(1L)

        val state = viewModel.uiState.value
        assertThat(state.isBusy).isFalse()
        assertThat(state.items.single().isCompleted).isFalse()
        assertThat(state.totalPoints).isEqualTo(0)
    }

    @Test
    fun write_cancelledWithCancellationException_resetsBusyWithoutBreakingState() = runTest {
        viewModel.addTodo("买牛奶")

        fake.errorFactory = { CancellationException("cancelled") }
        viewModel.addTodo("写周报")

        val state = viewModel.uiState.value
        assertThat(state.isBusy).isFalse()
        assertThat(state.items.map { it.title }).containsExactly("买牛奶")
    }
}
