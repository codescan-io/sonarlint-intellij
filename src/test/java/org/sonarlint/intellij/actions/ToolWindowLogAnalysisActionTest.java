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

import com.intellij.openapi.actionSystem.AnActionEvent;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.intellij.AbstractSonarLintLightTests;
import org.sonarlint.intellij.SonarLintTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

public class ToolWindowLogAnalysisActionTest extends AbstractSonarLintLightTests {
  private ToolWindowLogAnalysisAction action = new ToolWindowLogAnalysisAction();
  private AnActionEvent event;

  @Before
  public void prepare() {
    event = SonarLintTestUtils.createAnActionEvent(getProject());
  }

  @Test
  public void testSelected() {
    getProjectSettings().setAnalysisLogsEnabled(true);
    assertThat(action.isSelected(event)).isTrue();

    getProjectSettings().setAnalysisLogsEnabled(false);
    assertThat(action.isSelected(event)).isFalse();

    when(event.getProject()).thenReturn(null);
    assertThat(action.isSelected(event)).isFalse();
  }

  @Test
  public void testSetSelected() {
    getProjectSettings().setAnalysisLogsEnabled(true);

    action.setSelected(event, false);
    assertThat(getProjectSettings().isAnalysisLogsEnabled()).isFalse();

    action.setSelected(event, true);
    assertThat(getProjectSettings().isAnalysisLogsEnabled()).isTrue();

    // do nothing if there is no project
    when(event.getProject()).thenReturn(null);
    action.setSelected(event, false);
    assertThat(getProjectSettings().isAnalysisLogsEnabled()).isTrue();
  }
}
