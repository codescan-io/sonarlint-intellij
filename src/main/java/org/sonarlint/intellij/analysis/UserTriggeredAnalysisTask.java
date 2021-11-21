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
package org.sonarlint.intellij.analysis;

import com.intellij.openapi.progress.ProgressIndicator;
import org.jetbrains.annotations.NotNull;
import org.sonarlint.intellij.common.util.SonarLintUtils;

/**
 * A modal task (blocking) initiated explicitly by the user.
 */
class UserTriggeredAnalysisTask extends AnalysisTask {

  UserTriggeredAnalysisTask(AnalysisRequest request, boolean modal) {
    super(request, modal, false);
  }

  @Override public void run(@NotNull ProgressIndicator indicator) {
    try {
      super.run(indicator);
    } finally {
      if (!myProject.isDisposed()) {
        SonarLintUtils.getService(myProject, AnalysisStatus.class).stopRun();
      }
    }
  }
}
