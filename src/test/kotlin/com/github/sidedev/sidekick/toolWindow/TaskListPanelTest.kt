package com.github.sidedev.sidekick.toolWindow

import com.github.sidedev.sidekick.api.SidekickService
import com.github.sidedev.sidekick.api.Task
import com.github.sidedev.sidekick.api.response.ApiError
import com.github.sidedev.sidekick.api.response.ApiResponse
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest

@OptIn(ExperimentalCoroutinesApi::class)
class TaskListPanelTest : BasePlatformTestCase() {
    private lateinit var sidekickService: SidekickService
    private lateinit var taskListModel: TaskListModel
    private lateinit var taskListPanel: TaskListPanel
    private var taskSelectedCallbackInvoked = false
    private var newTaskCallbackInvoked = false
    
    private val testDispatcher = UnconfinedTestDispatcher()

    override fun setUp() {
        super.setUp()
        sidekickService = mockk()
        coEvery { sidekickService.getTasks("test-workspace") } returns ApiResponse.Success(emptyList())

        taskListModel = TaskListModel()
        taskListPanel = TaskListPanel(
            workspaceId = "test-workspace",
            taskListModel = taskListModel,
            sidekickService = sidekickService,
            onTaskSelected = { taskSelectedCallbackInvoked = true },
            onNewTask = { newTaskCallbackInvoked = true }
        )
    }

    fun testEmptyStateShowsNoTasksMessageAndButton() = runTest(testDispatcher) {
        // Given an empty task list
        taskListPanel.replaceTasks(emptyList())

        // And the New Task button should be present and visible
        assertTrue("New Task button should be visible", taskListPanel.newTaskButton.isVisible)
        assertEquals("New Task button should have correct text", "Start New Task", taskListPanel.newTaskButton.text)
    }

    fun testNewTaskButtonTriggersCallback() = runTest(testDispatcher) {
        // Given an empty task list
        taskListPanel.replaceTasks(emptyList())

        // When clicking the New Task button
        assertTrue("New Task button should be visible", taskListPanel.newTaskButton.isVisible)
        taskListPanel.newTaskButton.doClick()

        // Then the callback should be invoked
        assertTrue("New Task callback should be invoked", newTaskCallbackInvoked)
    }

    fun testTaskListDisplayWithNonEmptyTasks() = runTest(testDispatcher) {
        // Given a list of tasks
        val tasks = listOf(
            Task(
                id = "1",
                workspaceId = "test-workspace",
                status = "DONE",
                agentType = "test",
                flowType = "test",
                description = "Test task",
                created = "2023-01-01T00:00:00Z",
                updated = "2023-01-01T00:00:00Z"
            )
        )

        // When updating the task list
        taskListPanel.replaceTasks(tasks)

        // Then the task list should be visible and empty state hidden
        assertTrue("Task list should be visible", taskListPanel.taskList.isVisible)
        assertFalse("New Task button should be hidden", taskListPanel.newTaskButton.isVisible)
        assertEquals("Task list should have correct number of items", 1, taskListPanel.taskList.model.size)
    }

    fun testTaskSelectionTriggersCallback() = runTest(testDispatcher) {
        // Given a list of tasks
        val task = Task(
            id = "1",
            workspaceId = "test-workspace",
            status = "DONE",
            agentType = "test",
            flowType = "test",
            description = "Test task",
            created = "2023-01-01T00:00:00Z",
            updated = "2023-01-01T00:00:00Z"
        )
        taskListPanel.replaceTasks(listOf(task))

        // When selecting a task
        taskListPanel.taskList.selectedIndex = 0

        // Then the callback should be invoked
        assertTrue("Task selected callback should be invoked", taskSelectedCallbackInvoked)
    }

    fun testRefreshTaskListSuccess() = runTest(testDispatcher) {
        // Given a successful API response
        val tasks = listOf(
            Task(
                id = "1",
                workspaceId = "test-workspace",
                status = "DONE",
                agentType = "test",
                flowType = "test",
                description = "Test task",
                created = "2023-01-01T00:00:00Z",
                updated = "2023-01-01T00:00:00Z"
            )
        )
        coEvery { sidekickService.getTasks("test-workspace") } returns ApiResponse.Success(tasks)

        // When refreshing the task list
        runBlocking(testDispatcher) {
            taskListPanel.refreshTaskList()
        }

        // Then the tasks should be updated and status label hidden
        assertEquals("Task list should have correct number of items", 1, taskListPanel.taskList.model.size)
        assertFalse("Status label should be hidden", taskListPanel.statusLabel.isVisible)
    }

    fun testRefreshTaskListError() = runTest(testDispatcher) {
        // Given an initial task list
        val initialTask = Task(
            id = "1",
            workspaceId = "test-workspace",
            status = "DONE",
            agentType = "test",
            flowType = "test",
            description = "Initial task",
            created = "2023-01-01T00:00:00Z",
            updated = "2023-01-01T00:00:00Z"
        )
        taskListPanel.replaceTasks(listOf(initialTask))

        // And an API error response
        val errorMessage = "Failed to fetch tasks"
        coEvery { sidekickService.getTasks("test-workspace") } returns ApiResponse.Error(ApiError(errorMessage))

        // When refreshing the task list
        taskListPanel.refreshTaskList()

        // Then the error should be shown and existing tasks preserved
        assertEquals("Status label should show error message", errorMessage, taskListPanel.statusLabel.text)
        assertTrue("Status label should be visible", taskListPanel.statusLabel.isVisible)
        assertEquals("Task list should preserve existing tasks", 1, taskListPanel.taskList.model.size)
        assertEquals("Initial task should still be present", initialTask, taskListPanel.taskList.model.getElementAt(0))
    }
}