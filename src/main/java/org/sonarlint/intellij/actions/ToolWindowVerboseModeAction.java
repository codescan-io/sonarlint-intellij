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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;

import static org.sonarlint.intellij.config.Settings.getSettingsFor;

public class ToolWindowVerboseModeAction extends ToggleAction implements DumbAware {
  public ToolWindowVerboseModeAction() {
    super("Verbose output", "Enable verbose output for CodeScan analysis",
      AllIcons.Actions.StartDebugger);
  }

  @Override
  public boolean isSelected(AnActionEvent event) {
    Project p = event.getProject();
    return p != null && getSettingsFor(p).isVerboseEnabled();
  }

  @Override
  public void setSelected(AnActionEvent event, boolean flag) {
    Project p = event.getProject();
    if (p != null) {
      getSettingsFor(p).setVerboseEnabled(flag);
    }
  }
}
