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
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.vfs.VirtualFile;
import org.junit.Before;
import org.junit.Test;
import org.sonarlint.intellij.AbstractSonarLintLightTests;
import org.sonarlint.intellij.analysis.AnalysisCallback;
import org.sonarlint.intellij.analysis.AnalysisStatus;
import org.sonarlint.intellij.trigger.SonarLintSubmitter;
import org.sonarlint.intellij.trigger.TriggerType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class SonarAnalyzeFilesActionTest extends AbstractSonarLintLightTests {
  private SonarLintSubmitter submitter = mock(SonarLintSubmitter.class);
  private AnActionEvent event = mock(AnActionEvent.class);

  private Presentation presentation = new Presentation();
  private SonarAnalyzeFilesAction editorFileAction = new SonarAnalyzeFilesAction();

  @Before
  public void prepare() {
    replaceProjectService(SonarLintSubmitter.class, submitter);
    when(event.getProject()).thenReturn(getProject());
    when(event.getPresentation()).thenReturn(presentation);
  }

  @Test
  public void should_submit() {
    VirtualFile f1 = myFixture.copyFileToProject("foo.php", "foo.php");
    mockSelectedFiles(f1);
    editorFileAction.actionPerformed(event);
    verify(submitter).submitFiles(anyCollection(), eq(TriggerType.ACTION), any(AnalysisCallback.class), eq(false));
  }

  private void mockSelectedFiles(VirtualFile file) {
    when(event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)).thenReturn(new VirtualFile[] {file});
  }

  @Test
  public void should_do_nothing_if_no_file() {
    editorFileAction.actionPerformed(event);
    verifyZeroInteractions(submitter);
  }

  @Test
  public void should_do_nothing_if_no_project() {
    when(event.getProject()).thenReturn(null);
    editorFileAction.actionPerformed(event);
    verifyZeroInteractions(submitter);
  }

  @Test
  public void should_be_enabled_if_file_and_not_running() {
    VirtualFile f1 = myFixture.copyFileToProject("foo.php", "foo.php");
    mockSelectedFiles(f1);

    AnalysisStatus.get(getProject()).tryRun();

    editorFileAction.update(event);
    assertThat(presentation.isEnabled()).isFalse();

    AnalysisStatus.get(getProject()).stopRun();
    editorFileAction.update(event);
    assertThat(presentation.isEnabled()).isTrue();
  }
}
