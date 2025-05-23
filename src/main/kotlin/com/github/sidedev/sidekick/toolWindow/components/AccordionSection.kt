package com.github.sidedev.sidekick.toolWindow.components

import com.intellij.icons.AllIcons
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.Icon
import javax.swing.JComponent
import javax.swing.border.Border

open class AccordionSection(
    title: String,
    protected val content: JComponent,
    initiallyExpanded: Boolean = false
) : JBPanel<AccordionSection>(BorderLayout()) {
    companion object {
        const val MAX_WIDTH: Int = 800
        const val HEADER_HEIGHT: Int = 32
    }

    private var expanded: Boolean = initiallyExpanded
    private val headerLabel: JBLabel
    private val contentPanel: JBPanel<*> = JBPanel<JBPanel<*>>(BorderLayout())
    
    private val expandIcon: Icon = AllIcons.General.ChevronRight
    private val collapseIcon: Icon = AllIcons.General.ChevronDown
    
    private val sectionBorder: Border = (JBUI.Borders.compound(
        JBUI.Borders.customLine(
            JBUI.CurrentTheme.DefaultTabs.borderColor() 
                ?: JBUI.CurrentTheme.Label.foreground()
                ?: java.awt.Color.GRAY,
            0, 0, 1, 0
        ),
        JBUI.Borders.empty(8)
    ) ?: JBUI.Borders.empty())
    
    init {
        alignmentX = JComponent.LEFT_ALIGNMENT
        maximumSize = Dimension(MAX_WIDTH, if (expanded) Int.MAX_VALUE else HEADER_HEIGHT)
        minimumSize = Dimension(16, 10)

        // Configure header
        headerLabel = JBLabel(title).apply {
            icon = if (expanded) collapseIcon else expandIcon
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
            maximumSize = Dimension(MAX_WIDTH, 10)
            minimumSize = Dimension(16, 10)
            alignmentX = JComponent.LEFT_ALIGNMENT
            alignmentY = JComponent.CENTER_ALIGNMENT

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    toggleExpanded()
                }
            })
        }
        
        // Configure content panel
        contentPanel.apply {
            border = sectionBorder
            isVisible = expanded
            add(content, BorderLayout.NORTH)
        }
        
        // Add components to main panel
        add(headerLabel, BorderLayout.NORTH)
        add(contentPanel, BorderLayout.CENTER)
        
        // Set border for the entire section
        border = JBUI.Borders.empty(0)
    }

    fun setTitle(title: String) {
        headerLabel.text = title
        revalidate()
        repaint()
    }
    
    private fun toggleExpanded() {
        expanded = !expanded
        headerLabel.icon = if (expanded) collapseIcon else expandIcon
        contentPanel.isVisible = expanded
        maximumSize = Dimension(MAX_WIDTH, if (expanded) Int.MAX_VALUE else HEADER_HEIGHT)
        // Ensure proper repainting
        revalidate()
        repaint()
    }
    
    open fun setExpanded(expand: Boolean) { // Add 'open' keyword
        if (expanded != expand) {
            toggleExpanded()
        }
    }
    
    fun isExpanded(): Boolean = expanded
}