/*
 * CodeScan for IntelliJ IDEA
 * Copyright (C) 2015-2021 SonarSource
 * sonarlint@sonarsource.com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonarlint.intellij.ui.vulnerabilities

import com.intellij.icons.AllIcons.General.Information
import com.intellij.ide.OccurenceNavigator
import com.intellij.ide.OccurenceNavigator.OccurenceInfo
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataProvider
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.tools.SimpleActionGroup
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.components.labels.ActionLink
import com.intellij.ui.components.panels.HorizontalLayout
import com.intellij.util.ui.tree.TreeUtil
import org.sonarlint.intellij.actions.OpenIssueInBrowserAction
import org.sonarlint.intellij.actions.OpenTaintVulnerabilityDocumentationAction
import org.sonarlint.intellij.actions.RefreshTaintVulnerabilitiesAction
import org.sonarlint.intellij.actions.SonarConfigureProject
import org.sonarlint.intellij.common.util.SonarLintUtils.getService
import org.sonarlint.intellij.config.Settings.getGlobalSettings
import org.sonarlint.intellij.core.ProjectBindingManager
import org.sonarlint.intellij.editor.EditorDecorator
import org.sonarlint.intellij.issue.vulnerabilities.FoundTaintVulnerabilities
import org.sonarlint.intellij.issue.vulnerabilities.InvalidBinding
import org.sonarlint.intellij.issue.vulnerabilities.LocalTaintVulnerability
import org.sonarlint.intellij.issue.vulnerabilities.NoBinding
import org.sonarlint.intellij.issue.vulnerabilities.TaintVulnerabilitiesStatus
import org.sonarlint.intellij.ui.SonarLintRulePanel
import org.sonarlint.intellij.ui.SonarLintToolWindowFactory.createSplitter
import org.sonarlint.intellij.ui.nodes.AbstractNode
import org.sonarlint.intellij.ui.nodes.IssueNode
import org.sonarlint.intellij.ui.nodes.LocalTaintVulnerabilityNode
import org.sonarlint.intellij.ui.tree.TaintVulnerabilityTree
import org.sonarlint.intellij.ui.tree.TaintVulnerabilityTreeModelBuilder
import java.awt.BorderLayout
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.ScrollPaneConstants
import javax.swing.event.TreeSelectionListener
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.TreeNode
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel


private const val SPLIT_PROPORTION_PROPERTY = "SONARLINT_TAINT_VULNERABILITIES_SPLIT_PROPORTION"
private const val DEFAULT_SPLIT_PROPORTION = 0.5f

private const val NO_BINDING_CARD_ID = "NO_BINDING_CARD"
private const val INVALID_BINDING_CARD_ID = "INVALID_BINDING_CARD"
private const val NO_ISSUES_CARD_ID = "NO_ISSUES_CARD"
private const val TREE_CARD_ID = "TREE_CARD"

private const val TOOLBAR_GROUP_ID = "TaintVulnerabilities"

class TaintVulnerabilitiesPanel(private val project: Project) : SimpleToolWindowPanel(false, true),
    OccurenceNavigator, DataProvider, Disposable {

    private lateinit var rulePanel: SonarLintRulePanel
    private lateinit var tree: TaintVulnerabilityTree
    private lateinit var treeBuilder: TaintVulnerabilityTreeModelBuilder
    private val noVulnerabilitiesLabel = JLabel("")
    private val cards = CardPanel()

    class CardPanel {
        val container = JPanel()
        private var subPanels = mutableMapOf<String, JComponent>()

        init {
            container.layout = BoxLayout(container, BoxLayout.PAGE_AXIS)
        }

        fun add(panel: JComponent, id: String) {
            panel.isVisible = subPanels.isEmpty()
            panel.alignmentX = 0.5f
            panel.alignmentY = 0.5f
            container.add(panel)
            subPanels[id] = panel
        }

        fun show(id: String) {
            subPanels.values.forEach { it.isVisible = false }
            subPanels[id]!!.isVisible = true
        }
    }

    init {
        cards.add(centeredLabel("The project is not bound to SonarQube/SonarCloud", ActionLink("Configure Binding", SonarConfigureProject())), NO_BINDING_CARD_ID)
        cards.add(centeredLabel("The project binding is invalid", ActionLink("Edit Binding", SonarConfigureProject())), INVALID_BINDING_CARD_ID)
        cards.add(centeredLabel(noVulnerabilitiesLabel), NO_ISSUES_CARD_ID)
        cards.add(createSplitter(project, this, this, ScrollPaneFactory.createScrollPane(createTree()), createRulePanel(), SPLIT_PROPORTION_PROPERTY, DEFAULT_SPLIT_PROPORTION),
            TREE_CARD_ID
        )
        val issuesPanel = JPanel(BorderLayout())
        val globalSettings = getGlobalSettings()
        if (!globalSettings.isTaintVulnerabilitiesTabDisclaimerDismissed) {
            issuesPanel.add(createDisclaimer(), BorderLayout.NORTH)
        }
        issuesPanel.add(cards.container, BorderLayout.CENTER)
        setContent(issuesPanel)
        setupToolbar(listOf(RefreshTaintVulnerabilitiesAction(), OpenTaintVulnerabilityDocumentationAction()))
    }

    private fun centeredLabel(text: String, actionLink: ActionLink? = null) = centeredLabel(JLabel(text), actionLink)

    private fun centeredLabel(textLabel: JLabel, actionLink: ActionLink? = null): JPanel {
        val labelPanel = JPanel(HorizontalLayout(5))
        labelPanel.add(textLabel, HorizontalLayout.CENTER)
        if (actionLink != null) labelPanel.add(actionLink, HorizontalLayout.CENTER)
        return labelPanel
    }

    private fun createDisclaimer(): StripePanel {
        val stripePanel = StripePanel("This tab displays taint vulnerabilities detected by SonarQube or SonarCloud. SonarLint does not detect those issues locally.", Information)
        stripePanel.addAction("Learn more", OpenTaintVulnerabilityDocumentationAction())
        stripePanel.addAction("Dismiss", object : AnAction() {
            override fun actionPerformed(e: AnActionEvent) {
                stripePanel.isVisible = false
                getGlobalSettings().dismissTaintVulnerabilitiesTabDisclaimer()
            }
        })
        return stripePanel
    }

    private fun setupToolbar(actions: List<AnAction>) {
        val group = SimpleActionGroup()
        actions.forEach { group.add(it) }
        val toolbar = ActionManager.getInstance().createActionToolbar(TOOLBAR_GROUP_ID, group, false)
        toolbar.setTargetComponent(this)
        val toolBarBox = Box.createHorizontalBox()
        toolBarBox.add(toolbar.component)
        setToolbar(toolBarBox)
        toolbar.component.isVisible = true
    }

    private fun showCard(id: String) {
        cards.show(id)
    }

    fun populate(status: TaintVulnerabilitiesStatus) {
        val highlighting = getService(project, EditorDecorator::class.java)
        when (status) {
            is NoBinding -> {
                showCard(NO_BINDING_CARD_ID)
                highlighting.removeHighlights()
            }
            is InvalidBinding -> {
                showCard(INVALID_BINDING_CARD_ID)
                highlighting.removeHighlights()
            }
            is FoundTaintVulnerabilities -> {
                if (status.isEmpty()) {
                    showNoVulnerabilitiesLabel()
                    highlighting.removeHighlights()
                } else {
                    showCard(TREE_CARD_ID)
                    val expandedPaths = TreeUtil.collectExpandedPaths(tree)
                    val selectionPath: TreePath? = tree.selectionPath
                    // Temporarily remove the listener to avoid transcient selection events while changing the model
                    tree.removeTreeSelectionListener(TREE_SELECTION_LISTENER)
                    try {
                        treeBuilder.updateModel(status.byFile)
                        TreeUtil.restoreExpandedPaths(tree, expandedPaths)
                        if (selectionPath != null) {
                            TreeUtil.selectPath(tree, selectionPath)
                        } else {
                            expandDefault()
                        }
                    } finally {
                        tree.addTreeSelectionListener(TREE_SELECTION_LISTENER)
                        updateRulePanelContent()
                    }
                }
            }
        }
    }

    private fun expandDefault() {
        if (treeBuilder.numberIssues() < 30) {
            TreeUtil.expand(tree, 2)
        } else {
            tree.expandRow(0)
        }
    }

    private fun showNoVulnerabilitiesLabel() {
        val serverConnection = getService(project, ProjectBindingManager::class.java).serverConnection
        noVulnerabilitiesLabel.text = "No vulnerabilities found for currently opened files in the latest analysis on ${serverConnection.productName}."
        showCard(NO_ISSUES_CARD_ID)
    }

    fun setSelectedVulnerability(vulnerability: LocalTaintVulnerability) {
        val vulnerabilityNode = TreeUtil.findNode(tree.model.root as DefaultMutableTreeNode)
        { it is LocalTaintVulnerabilityNode && it.issue.key() == vulnerability.key() }
            ?: return
        TreeUtil.selectPath(tree, TreePath(vulnerabilityNode.path))
    }

    private fun createRulePanel(): JBTabbedPane {
        // Rule panel
        rulePanel = SonarLintRulePanel(project)

        val scrollableRulePanel = ScrollPaneFactory.createScrollPane(rulePanel.panel, true)
        scrollableRulePanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER)
        scrollableRulePanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED)
        scrollableRulePanel.verticalScrollBar.unitIncrement = 10
        val detailsTab = JBTabbedPane()
        detailsTab.addTab("Rule", null, scrollableRulePanel, "Details about the rule")
        return detailsTab
    }

    private fun updateRulePanelContent() {
        val highlighting = getService(project, EditorDecorator::class.java)
        val issue = tree.getIssueFromSelectedNode()
        if (issue == null) {
            rulePanel.setRuleKey(null, null)
            highlighting.removeHighlights()
        } else {
            val file = issue.file()
            if (file == null) {
                // FIXME can't we find a way to get the rule description?
                rulePanel.setRuleKey(null, null)
                highlighting.removeHighlights()
            } else {
                val moduleForFile = ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(file)
                rulePanel.setRuleKey(moduleForFile, issue.ruleKey())
                highlighting.highlight(issue)
            }
        }
    }

    private val TREE_SELECTION_LISTENER = TreeSelectionListener {
        updateRulePanelContent();
    };

    private fun createTree(): TaintVulnerabilityTree {
        treeBuilder = TaintVulnerabilityTreeModelBuilder()
        tree = TaintVulnerabilityTree(project, treeBuilder.model)
        tree.addTreeSelectionListener(TREE_SELECTION_LISTENER)
        tree.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (KeyEvent.VK_ESCAPE == e.keyCode) {
                    val highlighting = getService(
                        project,
                        EditorDecorator::class.java
                    )
                    highlighting.removeHighlights()
                }
            }
        })
        tree.selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        return tree
    }

    private fun occurrence(node: IssueNode?): OccurenceInfo? {
        if (node == null) {
            return null
        }
        val path = TreePath(node.path)
        tree.selectionModel.selectionPath = path
        tree.scrollPathToVisible(path)
        val range = node.issue().range
        val startOffset = range?.startOffset ?: 0
        return OccurenceInfo(
            OpenFileDescriptor(project, node.issue().psiFile().virtualFile, startOffset),
            -1,
            -1
        )
    }

    override fun hasNextOccurence(): Boolean {
        // relies on the assumption that a TreeNodes will always be the last row in the table view of the tree
        val path = tree.selectionPath ?: return false
        val node = path.lastPathComponent as DefaultMutableTreeNode
        return if (node is IssueNode) {
            tree.rowCount != tree.getRowForPath(path) + 1
        } else {
            node.childCount > 0
        }
    }

    override fun hasPreviousOccurence(): Boolean {
        val path = tree.selectionPath ?: return false
        val node = path.lastPathComponent as DefaultMutableTreeNode
        return node is IssueNode && !isFirst(node)
    }

    private fun isFirst(node: TreeNode): Boolean {
        val parent = node.parent
        return parent == null || parent.getIndex(node) == 0 && isFirst(parent)
    }

    override fun goNextOccurence(): OccurenceInfo? {
        val path = tree.selectionPath ?: return null
        return occurrence(treeBuilder.getNextIssue(path.lastPathComponent as AbstractNode))
    }

    override fun goPreviousOccurence(): OccurenceInfo? {
        val path = tree.selectionPath ?: return null
        return occurrence(treeBuilder.getPreviousIssue(path.lastPathComponent as AbstractNode))
    }

    override fun getNextOccurenceActionName(): String {
        return "Next Issue"
    }

    override fun getPreviousOccurenceActionName(): String {
        return "Previous Issue"
    }

    override fun getData(dataId: String): Any? {
        return if (OpenIssueInBrowserAction.TAINT_VULNERABILITY_DATA_KEY.`is`(dataId)) {
            tree.getSelectedIssue()
        } else null
    }

    override fun dispose() {
        // Nothing to do
    }

}
