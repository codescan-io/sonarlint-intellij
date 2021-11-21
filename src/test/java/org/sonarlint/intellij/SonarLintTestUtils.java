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
package org.sonarlint.intellij;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.Project;
import java.awt.GraphicsEnvironment;
import javax.annotation.Nullable;
import org.sonarsource.sonarlint.core.client.api.common.analysis.Issue;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SonarLintTestUtils {
  static {
    System.out.println("headless mode: " + GraphicsEnvironment.isHeadless());
  }

  private SonarLintTestUtils() {
    // only static
  }

  public static AnActionEvent createAnActionEvent(@Nullable Project project) {
    AnActionEvent event = mock(AnActionEvent.class);
    when(event.getProject()).thenReturn(project);
    return event;
  }

  public static Issue createIssue(int id) {
    Issue issue = mock(Issue.class);
    when(issue.getRuleKey()).thenReturn(Integer.toString(id));
    when(issue.getMessage()).thenReturn("issue " + id);
    return issue;
  }
}
