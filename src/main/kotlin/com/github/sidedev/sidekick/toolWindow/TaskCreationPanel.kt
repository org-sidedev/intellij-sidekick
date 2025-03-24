package com.github.sidedev.sidekick.toolWindow

import com.github.sidedev.sidekick.api.FlowOptions
import com.github.sidedev.sidekick.api.SidekickService
import com.github.sidedev.sidekick.api.TaskRequest
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.ui.Messages
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.panels.VerticalLayout
import com.intellij.util.ui.JBUI
import com.intellij.ui.components.panels.HorizontalLayout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.border.EmptyBorder

class TaskCreationPanel(
    private val sidekickService: SidekickService,
    private val workspaceId: String,
    private val onTaskCreated: () -> Unit,
) : JBPanel<JBPanel<*>>() {
    private val buttonValues = mutableMapOf<ActionLink, String>()

    internal val descriptionTextArea = JBTextArea().apply {
        lineWrap = true
        wrapStyleWord = true
        emptyText.text = "Task description - the more detail, the better"
        minimumSize = Dimension(300, 150)
        preferredSize = Dimension(300, 150)
    }

    internal val determineRequirementsCheckbox = JBCheckBox("Determine Requirements", true)

    internal val flowTypeButtons = createSegmentedButton(
        listOf("Just Code" to "basic_dev", "Plan Then Code" to "planned_dev"),
        getLastFlowType()
    )

    internal val createButton = JButton("Create Task").apply {
        addActionListener { createTask() }
    }

    init {
        layout = BorderLayout()
        border = EmptyBorder(JBUI.insets(10))

        val formPanel = JPanel(VerticalLayout(10)).apply {
            add(JBLabel("Status:"))
            add(JBLabel("Flow Type:"))
            add(flowTypeButtons)
            add(JBLabel("Description:"))
            add(JBScrollPane(descriptionTextArea))
            add(determineRequirementsCheckbox)
            add(createButton)
        }

        add(formPanel, BorderLayout.CENTER)
    }

    private fun createSegmentedButton(
        options: List<Pair<String, String>>,
        defaultValue: String
    ): JPanel {
        val panel = JPanel(HorizontalLayout(0))
        var selectedButton: ActionLink? = null

        options.forEach { (label, value) ->
            ActionLink(label).also { newButton ->
                buttonValues[newButton] = value
                newButton.addActionListener {
                    selectedButton?.foreground = JBColor.foreground()
                    newButton.foreground = JBColor.BLUE
                    selectedButton = newButton
                }
                if (value == defaultValue) {
                    newButton.foreground = JBColor.BLUE
                    selectedButton = newButton
                } else {
                    newButton.foreground = JBColor.foreground()
                }
                panel.add(newButton)
            }
        }

        return panel
    }

    private fun getSelectedValue(panel: JPanel): String {
        return panel.components
            .filterIsInstance<ActionLink>()
            .find { it.foreground == JBColor.BLUE }
            ?.let { buttonValues[it] }
            ?: throw IllegalStateException("No option selected")
    }

    private fun createTask() {
        val description = descriptionTextArea.text
        if (description.isEmpty()) {
            Messages.showErrorDialog(
                "Please enter a task description",
                "Validation Error"
            )
            return
        }

        val flowType = getSelectedValue(flowTypeButtons)

        // Store the selected flow type
        saveFlowType(flowType)

        val taskRequest = TaskRequest(
            description = description,
            status = "to_do",
            agentType = "llm",
            flowType = flowType,
            flowOptions = FlowOptions(
                determineRequirements = determineRequirementsCheckbox.isSelected
            )
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = sidekickService.createTask(workspaceId, taskRequest)
                if (response.isSuccess()) {
                    clearForm()
                    onTaskCreated()
                } else {
                    // FIXME don't use text area for error
                    descriptionTextArea.text = response.getErrorIfAny()?.error ?: "Unknown error"
                }
            } catch (e: Exception) {
                Messages.showErrorDialog(
                    "Failed to create task: ${e.message}",
                    "Error"
                )
            }
        }
    }

    private fun clearForm() {
        descriptionTextArea.text = ""
        determineRequirementsCheckbox.isSelected = true
    }

    companion object {
        private const val LAST_FLOW_TYPE_KEY = "sidekick.lastFlowType"
        private const val DEFAULT_FLOW_TYPE = "basic_dev"
    }

    private fun getLastFlowType(): String {
        return PropertiesComponent.getInstance().getValue(LAST_FLOW_TYPE_KEY, DEFAULT_FLOW_TYPE)
    }

    private fun saveFlowType(flowType: String) {
        PropertiesComponent.getInstance().setValue(LAST_FLOW_TYPE_KEY, flowType)
    }
}