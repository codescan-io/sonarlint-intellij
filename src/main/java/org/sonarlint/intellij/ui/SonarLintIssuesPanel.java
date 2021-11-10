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

import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.JBSplitter;
import com.intellij.ui.ScrollPaneFactory;
import com.intellij.util.ui.tree.TreeUtil;

import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javax.annotation.Nullable;
import javax.swing.JPanel;

import org.jetbrains.annotations.NonNls;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.issue.LiveIssue;
import org.sonarlint.intellij.util.SonarLintActions;

import static org.sonarlint.intellij.ui.SonarLintToolWindowFactory.createSplitter;

public class SonarLintIssuesPanel extends AbstractIssuesPanel implements Disposable {
  private static final String SPLIT_PROPORTION_PROPERTY = "SONARLINT_ISSUES_SPLIT_PROPORTION";
  private final CurrentFileController scope;
  private final JBSplitter splitter;

  public SonarLintIssuesPanel(Project project, CurrentFileController scope) {
    super(project);
    this.scope = scope;

    // Issues panel
    setToolbar(actions());
    JPanel issuesPanel = new JPanel(new BorderLayout());
    issuesPanel.add(ScrollPaneFactory.createScrollPane(tree), BorderLayout.CENTER);
    issuesPanel.add(new AutoTriggerStatusPanel(project).getPanel(), BorderLayout.SOUTH);

    splitter = createSplitter(project, this, this, issuesPanel, detailsTab, SPLIT_PROPORTION_PROPERTY, 0.5f);
    super.setContent(splitter);
    subscribeToEvents();
  }

  @Override
  public void dispose() {
    // Nothing to do
  }

  private static Collection<AnAction> actions() {
    SonarLintActions sonarLintActions = SonarLintActions.getInstance();
    List<AnAction> list = new ArrayList<>();
    list.add(ActionManager.getInstance().getAction("CodeScan.AnalyzeFiles"));
    list.add(ActionManager.getInstance().getAction("CodeScan.toolwindow.Cancel"));
    list.add(ActionManager.getInstance().getAction("CodeScan.toolwindow.Configure"));
    list.add(sonarLintActions.clearIssues());

    return list;
  }

  private void subscribeToEvents() {
    scope.setPanel(this);
  }

  public void update(@Nullable VirtualFile file, Collection<LiveIssue> issues, String emptyText) {
    if (file == null) {
      treeBuilder.updateModel(Collections.emptyMap(), emptyText);
    } else {
      treeBuilder.updateModel(Collections.singletonMap(file, issues), emptyText);
    }
    expandTree();
  }

  private void expandTree() {
    TreeUtil.expandAll(tree);
  }

  @Nullable
  @Override
  public Object getData(@NonNls String dataId) {
    // workaround for https://youtrack.jetbrains.com/issue/IDEA-262818
    // remove if fixed before the official 2021.1
    if (!EventQueue.isDispatchThread()) {
      return null;
    }
    if (CommonDataKeys.VIRTUAL_FILE.is(dataId)) {
      return SonarLintUtils.getSelectedFile(project);
    }

    return null;
  }
}
