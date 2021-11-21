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

import org.junit.Before;
import org.junit.Test;
import org.sonarlint.intellij.AbstractSonarLintLightTests;
import org.sonarlint.intellij.common.ui.SonarLintConsole;
import org.sonarsource.sonarlint.core.client.api.common.LogOutput;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class GlobalLogOutputImplTest extends AbstractSonarLintLightTests {
  private GlobalLogOutputImpl output;

  @Before
  public void prepare() {
    output = new GlobalLogOutputImpl();
  }

  @Test
  public void should_log_to_registered_consoles() {
    SonarLintConsole console = mock(SonarLintConsole.class);
    replaceProjectService(SonarLintConsole.class, console);
    output.log("warn", LogOutput.Level.WARN);
    verify(console).info("warn");

    output.log("info", LogOutput.Level.INFO);
    verify(console).info("info");

    output.log("debug", LogOutput.Level.DEBUG);
    verify(console).debug("debug");

    output.log("error", LogOutput.Level.ERROR);
    verify(console).error("error");

    output.log("trace", LogOutput.Level.TRACE);
    verify(console).debug("trace");
  }

}
