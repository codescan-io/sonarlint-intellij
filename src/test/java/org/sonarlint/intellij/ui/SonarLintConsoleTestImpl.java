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

import com.intellij.execution.ui.ConsoleView;
import org.sonarlint.intellij.common.ui.SonarLintConsole;

public class SonarLintConsoleTestImpl implements SonarLintConsole {

  private String lastMessage = "";

  public String getLastMessage() {
    return lastMessage;
  }

  @Override
  public void debug(String msg) {
    lastMessage = msg;
  }

  @Override
  public boolean debugEnabled() {
    return true;
  }

  @Override
  public void info(String msg) {
    lastMessage = msg;
  }

  @Override
  public void error(String msg) {
    lastMessage = msg;
  }

  @Override
  public void error(String msg, Throwable t) {
    lastMessage = msg;
  }

  @Override
  public void clear() {
    lastMessage = "";
  }

  @Override
  public ConsoleView getConsoleView() {
    return null;
  }
}
