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

import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.serviceContainer.NonInjectable;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.jetbrains.annotations.Nullable;
import org.sonarlint.intellij.common.ui.SonarLintConsole;

import static org.sonarlint.intellij.config.Settings.getSettingsFor;

public class SonarLintConsoleImpl implements SonarLintConsole, Disposable {

  private ConsoleView consoleView;
  private final Project myProject;

  public SonarLintConsoleImpl(Project project) {
    this.myProject = project;
  }

  @NonInjectable
  SonarLintConsoleImpl(Project project, ConsoleView consoleView) {
    this.consoleView = consoleView;
    this.myProject = project;
  }

  @Override
  public void debug(String msg) {
    if (debugEnabled()) {
      getConsoleView().print(msg + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
    }
  }

  @Override
  public boolean debugEnabled() {
    return getSettingsFor(myProject).isVerboseEnabled();
  }

  @Override
  public void info(String msg) {
    getConsoleView().print(msg + "\n", ConsoleViewContentType.NORMAL_OUTPUT);
  }

  @Override
  public void error(String msg) {
    getConsoleView().print(msg + "\n", ConsoleViewContentType.ERROR_OUTPUT);
  }

  @Override
  public void error(String msg, @Nullable Throwable t) {
    error(msg);
    if (t != null) {
      StringWriter errors = new StringWriter();
      t.printStackTrace(new PrintWriter(errors));
      error(errors.toString());
    }
  }

  @Override
  public synchronized void clear() {
    if (consoleView != null) {
      consoleView.clear();
    }
  }

  @Override
  public synchronized ConsoleView getConsoleView() {
    if (consoleView == null) {
      consoleView = TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole();
    }
    return this.consoleView;
  }

  @Override
  public void dispose() {
    if (consoleView != null) {
      Disposer.dispose(consoleView);
    }
  }
}
