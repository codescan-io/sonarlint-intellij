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
package org.sonarlint.intellij.ui;

import com.intellij.ide.OccurenceNavigator;
import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.editor.RangeMarker;
import com.intellij.openapi.fileEditor.OpenFileDescriptor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.ui.SimpleToolWindowPanel;
import com.intellij.tools.SimpleActionGroup;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.ui.components.JBTabbedPane;
import com.intellij.ui.treeStructure.Tree;
import com.intellij.util.ui.tree.TreeUtil;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Collection;
import javax.annotation.CheckForNull;
import javax.annotation.Nullable;
import javax.swing.Box;
import javax.swing.JScrollPane;
import javax.swing.ScrollPaneConstants;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.editor.EditorDecorator;
import org.sonarlint.intellij.issue.LiveIssue;
import org.sonarlint.intellij.ui.nodes.AbstractNode;
import org.sonarlint.intellij.ui.nodes.IssueNode;
import org.sonarlint.intellij.ui.tree.FlowsTree;
import org.sonarlint.intellij.ui.tree.FlowsTreeModelBuilder;
import org.sonarlint.intellij.ui.tree.IssueTree;
import org.sonarlint.intellij.ui.tree.IssueTreeModelBuilder;

public abstract class AbstractIssuesPanel extends SimpleToolWindowPanel implements OccurenceNavigator {
  private static final String ID = "CodeScan";
  private static final int RULE_TAB_INDEX = 0;
  private static final int LOCATIONS_TAB_INDEX = 1;
  protected final Project project;
  protected SonarLintRulePanel rulePanel;
  protected JBTabbedPane detailsTab;
  protected Tree tree;
  protected IssueTreeModelBuilder treeBuilder;
  protected FlowsTree flowsTree;
  protected FlowsTreeModelBuilder flowsTreeBuilder;
  private ActionToolbar mainToolbar;

  protected AbstractIssuesPanel(Project project) {
    super(false, true);
    this.project = project;

    createFlowsTree();
    createIssuesTree();
    createTabs();
  }

  public void refreshToolbar() {
    mainToolbar.updateActionsImmediately();
  }

  private void createTabs() {
    // Flows panel with tree
    JScrollPane flowsPanel = ScrollPaneFactory.createScrollPane(flowsTree, true);
    flowsPanel.getVerticalScrollBar().setUnitIncrement(10);

    // Rule panel
    rulePanel = new SonarLintRulePanel(project);
    JScrollPane scrollableRulePanel = ScrollPaneFactory.createScrollPane(rulePanel.getPanel(), true);

    scrollableRulePanel.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
    scrollableRulePanel.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
    scrollableRulePanel.getVerticalScrollBar().setUnitIncrement(10);

    detailsTab = new JBTabbedPane();
    detailsTab.insertTab("Rule", null, scrollableRulePanel, "Details about the rule", RULE_TAB_INDEX);
    detailsTab.insertTab("Locations", null, flowsPanel, "All locations involved in the issue", LOCATIONS_TAB_INDEX);
  }

  protected void issueTreeSelectionChanged() {
    IssueNode[] selectedNodes = tree.getSelectedNodes(IssueNode.class, null);
    if (selectedNodes.length > 0) {
      LiveIssue issue = selectedNodes[0].issue();
      Module moduleForFile = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(issue.psiFile().getVirtualFile());
      rulePanel.setRuleKey(moduleForFile, issue.getRuleKey());
      SonarLintUtils.getService(project, EditorDecorator.class).highlightIssue(issue);
      flowsTree.getEmptyText().setText("Selected issue doesn't have flows");
      flowsTreeBuilder.populateForIssue(issue);
      flowsTree.expandAll();
    } else {
      flowsTreeBuilder.clearFlows();
      flowsTree.getEmptyText().setText("No issue selected");
      rulePanel.setRuleKey(null, null);
      EditorDecorator highlighting = SonarLintUtils.getService(project, EditorDecorator.class);
      highlighting.removeHighlights();
    }
  }

  protected void setToolbar(Collection<AnAction> actions) {
    setToolbar(createActionGroup(actions));
  }

  protected void setToolbar(ActionGroup group) {
    if (mainToolbar != null) {
      mainToolbar.setTargetComponent(null);
      super.setToolbar(null);
      mainToolbar = null;
    }
    mainToolbar = ActionManager.getInstance().createActionToolbar(ID, group, false);
    mainToolbar.setTargetComponent(this);
    Box toolBarBox = Box.createHorizontalBox();
    toolBarBox.add(mainToolbar.getComponent());
    super.setToolbar(toolBarBox);
    mainToolbar.getComponent().setVisible(true);
  }

  private static ActionGroup createActionGroup(Collection<AnAction> actions) {
    SimpleActionGroup actionGroup = new SimpleActionGroup();
    actions.forEach(actionGroup::add);
    return actionGroup;
  }

  private void createFlowsTree() {
    flowsTreeBuilder = new FlowsTreeModelBuilder();
    DefaultTreeModel model = flowsTreeBuilder.createModel();
    flowsTree = new FlowsTree(project, model);
    flowsTreeBuilder.clearFlows();
    flowsTree.getEmptyText().setText("No issue selected");
  }

  private void createIssuesTree() {
    treeBuilder = new IssueTreeModelBuilder();
    DefaultTreeModel model = treeBuilder.createModel();
    tree = new IssueTree(project, model);
    tree.addTreeSelectionListener(e -> issueTreeSelectionChanged());
    tree.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (KeyEvent.VK_ESCAPE == e.getKeyCode()) {
          EditorDecorator highlighting = SonarLintUtils.getService(project, EditorDecorator.class);
          highlighting.removeHighlights();
        }
      }
    });
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
  }

  @CheckForNull
  private OccurenceNavigator.OccurenceInfo occurrence(@Nullable IssueNode node) {
    if (node == null) {
      return null;
    }

    TreePath path = new TreePath(node.getPath());
    tree.getSelectionModel().setSelectionPath(path);
    tree.scrollPathToVisible(path);

    RangeMarker range = node.issue().getRange();
    int startOffset = (range != null) ? range.getStartOffset() : 0;
    return new OccurenceNavigator.OccurenceInfo(
      new OpenFileDescriptor(project, node.issue().psiFile().getVirtualFile(), startOffset),
      -1,
      -1);
  }

  @Override
  public boolean hasNextOccurence() {
    // relies on the assumption that a TreeNodes will always be the last row in the table view of the tree
    TreePath path = tree.getSelectionPath();
    if (path == null) {
      return false;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
    if (node instanceof IssueNode) {
      return tree.getRowCount() != tree.getRowForPath(path) + 1;
    } else {
      return node.getChildCount() > 0;
    }
  }

  @Override
  public boolean hasPreviousOccurence() {
    TreePath path = tree.getSelectionPath();
    if (path == null) {
      return false;
    }
    DefaultMutableTreeNode node = (DefaultMutableTreeNode) path.getLastPathComponent();
    return (node instanceof IssueNode) && !isFirst(node);
  }

  private static boolean isFirst(final TreeNode node) {
    final TreeNode parent = node.getParent();
    return parent == null || (parent.getIndex(node) == 0 && isFirst(parent));
  }

  @CheckForNull
  @Override
  public OccurenceNavigator.OccurenceInfo goNextOccurence() {
    TreePath path = tree.getSelectionPath();
    if (path == null) {
      return null;
    }
    return occurrence(treeBuilder.getNextIssue((AbstractNode) path.getLastPathComponent()));
  }

  @CheckForNull
  @Override
  public OccurenceNavigator.OccurenceInfo goPreviousOccurence() {
    TreePath path = tree.getSelectionPath();
    if (path == null) {
      return null;
    }
    return occurrence(treeBuilder.getPreviousIssue((AbstractNode) path.getLastPathComponent()));
  }

  @Override
  public String getNextOccurenceActionName() {
    return "Next Issue";
  }

  @Override
  public String getPreviousOccurenceActionName() {
    return "Previous Issue";
  }

  public void setSelectedIssue(LiveIssue issue) {
    DefaultMutableTreeNode issueNode = TreeUtil.findNode(((DefaultMutableTreeNode) tree.getModel().getRoot()),
      (node) -> node instanceof IssueNode && ((IssueNode) node).issue().equals(issue));
    if (issueNode == null) {
      return;
    }
    tree.setSelectionPath(null);
    tree.addSelectionPath(new TreePath(issueNode.getPath()));
  }

  public void selectLocationsTab() {
    detailsTab.setSelectedIndex(LOCATIONS_TAB_INDEX);
  }

  public void selectRulesTab() {
    detailsTab.setSelectedIndex(RULE_TAB_INDEX);
  }

}
