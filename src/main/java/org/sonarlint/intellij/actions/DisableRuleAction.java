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
package org.sonarlint.intellij.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.project.Project;
import org.sonarlint.intellij.common.util.SonarLintUtils;
import org.sonarlint.intellij.issue.LiveIssue;
import org.sonarlint.intellij.trigger.SonarLintSubmitter;
import org.sonarlint.intellij.trigger.TriggerType;

import static org.sonarlint.intellij.config.Settings.getGlobalSettings;
import static org.sonarlint.intellij.config.Settings.getSettingsFor;

public class DisableRuleAction extends AnAction {
  public static final DataKey<LiveIssue> ISSUE_DATA_KEY = DataKey.create("sonarlint_issue");

  public DisableRuleAction() {
    super("Disable Rule", "Disable the CodeScan rule that activated this rule", AllIcons.Actions.Cancel);
  }

  @Override public void actionPerformed(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      return;
    }

    LiveIssue issue = e.getData(ISSUE_DATA_KEY);
    if (issue != null) {
      disableRule(issue.getRuleKey());
      SonarLintSubmitter submitter = SonarLintUtils.getService(project, SonarLintSubmitter.class);
      submitter.submitOpenFilesAuto(TriggerType.BINDING_UPDATE);
    }
  }

  @Override
  public void update(AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      e.getPresentation().setEnabled(false);
      e.getPresentation().setVisible(false);
      return;
    }

    LiveIssue issue = e.getData(ISSUE_DATA_KEY);
    boolean visible = !getSettingsFor(project).isBindingEnabled() && issue != null;
    e.getPresentation().setVisible(visible);

    boolean explicitlyDisabled = issue != null && getGlobalSettings().isRuleExplicitlyDisabled(issue.getRuleKey());
    boolean enabled = visible && !explicitlyDisabled;
    e.getPresentation().setEnabled(enabled);
  }

  private static void disableRule(String ruleKey) {
    getGlobalSettings().disableRule(ruleKey);
  }

}
