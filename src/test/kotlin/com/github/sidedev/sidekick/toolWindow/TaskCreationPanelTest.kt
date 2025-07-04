package com.github.sidedev.sidekick.toolWindow

import com.github.sidedev.sidekick.api.AgentType
import com.github.sidedev.sidekick.api.Task
import com.github.sidedev.sidekick.api.TaskStatus
import com.github.sidedev.sidekick.api.response.ApiError
import com.github.sidedev.sidekick.api.response.ApiResponse
import com.github.sidedev.sidekick.testing.SidekickBaseTestCase
import io.mockk.coEvery
import io.mockk.coVerify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock

@OptIn(ExperimentalCoroutinesApi::class)
class TaskCreationPanelTest : SidekickBaseTestCase() {
    private lateinit var taskCreationPanel: TaskCreationPanel
    private var taskCreatedCallbackInvoked = false

    // the unconfined test dispatcher makes tests much simpler, given we don't
    // care about coroutine ordering etc., but just want to run coroutines to
    // completion typically, then assert
    private val testDispatcher = UnconfinedTestDispatcher()

    override fun setUp() {
        super.setUp()

        taskCreatedCallbackInvoked = false

        taskCreationPanel = TaskCreationPanel(
            sidekickService = sidekickService,
            workspaceId = "ws_123",
            onTaskCreated = { _ -> taskCreatedCallbackInvoked = true },
            dispatcher = testDispatcher,
        )
    }

    fun testEmptyDescriptionShowsError() = runTest(testDispatcher) {
        // When: Click create button without entering description
        taskCreationPanel.createButton.doClick()

        // Then: Error is shown
        assertTrue(taskCreationPanel.errorLabel.isVisible)
        assertEquals("<html>Please enter a task description</html>", taskCreationPanel.errorLabel.text)

        // And: API was not called
        coVerify(exactly = 0) {
            sidekickService.createTask(any(), any())
        }

        // And: Callback was not invoked
        assertFalse(taskCreatedCallbackInvoked)
    }

    fun testApiErrorShowsErrorMessage() = runTest(testDispatcher) {
        // Given: Mock service that will return error
        coEvery {
            sidekickService.createTask(any(), any())
        } returns ApiResponse.Error(error = ApiError("API Error Message"))

        // When: Fill the form
        taskCreationPanel.descriptionTextArea.text = "Test description"

        // And: Click create button
        taskCreationPanel.createButton.doClick()

        // Then: Error is shown
        assertTrue(taskCreationPanel.errorLabel.isVisible)
        assertEquals("<html>API Error Message</html>", taskCreationPanel.errorLabel.text)

        // And: Form is not cleared
        assertEquals("Test description", taskCreationPanel.descriptionTextArea.text)

        // And: Callback was not invoked
        assertFalse(taskCreatedCallbackInvoked)
    }

    fun testCreateTaskClearsFormWithoutApiCall() = runTest(testDispatcher) {
        val task = Task(
            id = "1",
            description = "test",
            workspaceId = "ws_123",
            status = TaskStatus.TO_DO,
            agentType = AgentType.LLM,
            flowType = "basic_dev",
            created = Clock.System.now(),
            updated = Clock.System.now(),
        )

        // Given: Mock service that will return success
        coEvery {
            sidekickService.createTask(
                any(),
                any(),
            )
        } returns ApiResponse.Success(task)

        // When: Fill the form
        val description = "Test task description"
        taskCreationPanel.descriptionTextArea.text = description

        // And: Click create button
        taskCreationPanel.createButton.doClick()

        // Then: Form is cleared
        assertEquals("", taskCreationPanel.descriptionTextArea.text)
        assertTrue(taskCreationPanel.determineRequirementsCheckbox.isSelected)

        // And: Callback was invoked with the task
        assertTrue(taskCreatedCallbackInvoked)

        // And: API was called
        coVerify(exactly = 1) {
            sidekickService.createTask(
                workspaceId = "ws_123",
                any(),
            )
        }

        // And callback was invoked
        assertTrue(taskCreatedCallbackInvoked)
    }
}
