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
package org.sonarlint.intellij.util;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.serviceContainer.NonInjectable;
import icons.SonarLintIcons;
import org.sonarlint.intellij.actions.SonarAnalyzeAllFilesAction;
import org.sonarlint.intellij.actions.SonarAnalyzeChangedFilesAction;
import org.sonarlint.intellij.actions.SonarCleanConsoleAction;
import org.sonarlint.intellij.actions.SonarClearAnalysisResultsAction;
import org.sonarlint.intellij.actions.SonarClearIssuesAction;
import org.sonarlint.intellij.common.util.SonarLintUtils;

/**
 * Creates and keeps a single instance of actions used by CodeScan.
 * Some actions are created programmatically instead of being declared in plugin.xml so that they are not registered in
 * ActionManager, becoming accessible from the action search.
 */
public class SonarLintActions {

  private final AnAction clearResultsAction;
  private final AnAction clearIssuesAction;
  private final AnAction cleanConsoleAction;
  private final AnAction cancelAction;
  private final AnAction configureAction;
  private final AnAction analyzeChangedFilesAction;
  private final AnAction analyzeAllFilesAction;

  public SonarLintActions() {
    this(ActionManager.getInstance());
  }

  @NonInjectable
  SonarLintActions(ActionManager actionManager) {
    AnAction analyzeMenu = actionManager.getAction("AnalyzeMenu");
    // some flavors of IDEA don't have the analyze menu
    if (analyzeMenu instanceof DefaultActionGroup) {
      AnAction sonarLintAnalyzeMenu = actionManager.getAction("CodeScan.AnalyzeMenu");
      DefaultActionGroup analyzeMenuGroup = (DefaultActionGroup) analyzeMenu;
      analyzeMenuGroup.add(sonarLintAnalyzeMenu);
    }

    cancelAction = actionManager.getAction("CodeScan.toolwindow.Cancel");
    configureAction = actionManager.getAction("CodeScan.toolwindow.Configure");

    clearResultsAction = new SonarClearAnalysisResultsAction("Clear Project Files Issues",
      "Clear analysis results",
      SonarLintIcons.CLEAN);
    clearIssuesAction = new SonarClearIssuesAction("Clear CodeScan Issues",
      "Clear CodeScan issues",
      SonarLintIcons.CLEAN);
    cleanConsoleAction = new SonarCleanConsoleAction("Clear CodeScan Console",
      "Clear CodeScan console",
      SonarLintIcons.CLEAN);
    analyzeAllFilesAction = new SonarAnalyzeAllFilesAction("Analyze All Project Files",
      "Run a CodeScan analysis on all project files",
      SonarLintIcons.PROJECT);
    analyzeChangedFilesAction = new SonarAnalyzeChangedFilesAction("Analyze VCS Changed Files",
      "Run a CodeScan analysis on VCS changed files",
      SonarLintIcons.SCM);
  }

  public static SonarLintActions getInstance() {
    return SonarLintUtils.getService(SonarLintActions.class);
  }

  public AnAction cancelAnalysis() {
    return cancelAction;
  }

  public AnAction clearResults() {
    return clearResultsAction;
  }

  public AnAction clearIssues() {
    return clearIssuesAction;
  }

  public AnAction configure() {
    return configureAction;
  }

  public AnAction cleanConsole() {
    return cleanConsoleAction;
  }

  public AnAction analyzeChangedFiles() {
    return analyzeChangedFilesAction;
  }

  public AnAction analyzeAllFiles() {
    return analyzeAllFilesAction;
  }

}
